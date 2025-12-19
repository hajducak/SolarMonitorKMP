package com.solarmonitor.data.repository

import com.solarmonitor.data.modbus.ModbusClient
import com.solarmonitor.data.modbus.ModbusException
import com.solarmonitor.domain.models.DeviceConfiguration
import com.solarmonitor.domain.models.DeviceInfo
import com.solarmonitor.domain.models.SolarPanelData
import com.solarmonitor.domain.repository.SolarDeviceRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of SolarDeviceRepository
 * Manages device connections, data collection, and cloud sync
 */
class SolarDeviceRepositoryImpl : SolarDeviceRepository {
    
    private val devices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    private val activeClients = mutableMapOf<String, ModbusClient>()
    private val dataStreams = mutableMapOf<String, MutableSharedFlow<SolarPanelData>>()
    private val pollingJobs = mutableMapOf<String, Job>()
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // IP range to scan for devices (adjust based on your network)
    private val ipBase = "10.10.100"
    private val ipRange = 250..254  // Scan from .250 to .254
    
    override suspend fun discoverDevices(): List<DeviceInfo> = withContext(Dispatchers.IO) {
        val discoveredDevices = mutableListOf<DeviceInfo>()
        
        println("Starting device discovery on network $ipBase.x...")
        
        // Scan IP range in parallel
        coroutineScope {
            ipRange.map { lastOctet ->
                async {
                    val ip = "$ipBase.$lastOctet"
                    tryConnectToDevice(ip)
                }
            }.awaitAll().filterNotNull().let { discoveredDevices.addAll(it) }
        }
        
        println("Discovery complete. Found ${discoveredDevices.size} devices")
        discoveredDevices
    }
    
    /**
     * Attempts to connect to a device at the given IP
     */
    private suspend fun tryConnectToDevice(ip: String, port: Int = 8893): DeviceInfo? {
        return try {
            val testDevice = DeviceInfo(
                id = "device_${ip.replace(".", "_")}",
                name = "Solar Panel $ip",
                ipAddress = ip,
                port = port,
                slaveId = 4
            )
            
            val client = ModbusClient(testDevice, useTLS = false, timeout = 2000L)
            
            if (client.connect()) {
                println("Found device at $ip:$port")
                // Try to read data to verify it's a solar device
                try {
                    client.readSolarData()
                    testDevice.copy(isOnline = true, lastSeen = System.currentTimeMillis())
                } catch (e: Exception) {
                    println("Device at $ip responded but data read failed: ${e.message}")
                    null
                } finally {
                    client.disconnect()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null // Device not found at this IP
        }
    }
    
    override fun getDevices(): Flow<List<DeviceInfo>> = devices.asStateFlow()
    
    override suspend fun getDevice(deviceId: String): DeviceInfo? {
        return devices.value.find { it.id == deviceId }
    }
    
    override suspend fun addDevice(device: DeviceInfo): Boolean {
        return try {
            val currentDevices = devices.value.toMutableList()
            if (currentDevices.none { it.id == device.id }) {
                currentDevices.add(device)
                devices.value = currentDevices
                println("Device added: ${device.name}")
                true
            } else {
                println("Device already exists: ${device.name}")
                false
            }
        } catch (e: Exception) {
            println("Error adding device: ${e.message}")
            false
        }
    }
    
    override suspend fun updateDevice(device: DeviceInfo): Boolean {
        return try {
            val currentDevices = devices.value.toMutableList()
            val index = currentDevices.indexOfFirst { it.id == device.id }
            if (index >= 0) {
                currentDevices[index] = device
                devices.value = currentDevices
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Error updating device: ${e.message}")
            false
        }
    }
    
    override suspend fun removeDevice(deviceId: String): Boolean {
        return try {
            disconnectFromDevice(deviceId)
            val currentDevices = devices.value.toMutableList()
            val removed = currentDevices.removeIf { it.id == deviceId }
            if (removed) {
                devices.value = currentDevices
            }
            removed
        } catch (e: Exception) {
            println("Error removing device: ${e.message}")
            false
        }
    }
    
    override suspend fun connectToDevice(deviceId: String): Boolean {
        val device = getDevice(deviceId) ?: return false
        
        // Disconnect if already connected
        if (activeClients.containsKey(deviceId)) {
            disconnectFromDevice(deviceId)
        }
        
        return try {
            val client = ModbusClient(device, useTLS = true, timeout = 5000L)
            
            if (client.connect()) {
                activeClients[deviceId] = client
                
                // Create data stream for this device
                val dataFlow = MutableSharedFlow<SolarPanelData>(replay = 1)
                dataStreams[deviceId] = dataFlow
                
                // Start polling data from device
                startDataPolling(deviceId, client, dataFlow)
                
                // Update device status
                updateDevice(device.copy(isOnline = true, lastSeen = System.currentTimeMillis()))
                
                println("Connected to device: ${device.name}")
                true
            } else {
                println("Failed to connect to device: ${device.name}")
                false
            }
        } catch (e: Exception) {
            println("Error connecting to device: ${e.message}")
            false
        }
    }
    
    override suspend fun disconnectFromDevice(deviceId: String) {
        pollingJobs[deviceId]?.cancel()
        pollingJobs.remove(deviceId)
        
        activeClients[deviceId]?.disconnect()
        activeClients.remove(deviceId)
        
        dataStreams.remove(deviceId)
        
        getDevice(deviceId)?.let { device ->
            updateDevice(device.copy(isOnline = false))
        }
        
        println("Disconnected from device: $deviceId")
    }
    
    /**
     * Starts continuous data polling from a device
     */
    private fun startDataPolling(
        deviceId: String,
        client: ModbusClient,
        dataFlow: MutableSharedFlow<SolarPanelData>
    ) {
        pollingJobs[deviceId]?.cancel()
        
        pollingJobs[deviceId] = scope.launch {
            while (isActive && client.isConnected()) {
                try {
                    val data = client.readSolarData()
                    dataFlow.emit(data)
                    
                    // Update last seen time
                    getDevice(deviceId)?.let { device ->
                        updateDevice(device.copy(lastSeen = System.currentTimeMillis()))
                    }
                    
                    // Optionally sync to cloud
                    // syncToCloud(deviceId, data)
                    
                    delay(1.seconds) // Poll every second
                } catch (e: ModbusException) {
                    println("Error reading from device $deviceId: ${e.message}")
                    delay(5.seconds) // Wait longer on error
                } catch (e: CancellationException) {
                    break
                }
            }
        }
    }
    
    override fun getDeviceDataStream(deviceId: String): Flow<SolarPanelData> {
        return dataStreams[deviceId]?.asSharedFlow() ?: emptyFlow()
    }
    
    override suspend fun readDeviceData(deviceId: String): SolarPanelData? {
        val client = activeClients[deviceId] ?: return null
        
        return try {
            client.readSolarData()
        } catch (e: Exception) {
            println("Error reading device data: ${e.message}")
            null
        }
    }
    
    override suspend fun readDeviceConfiguration(deviceId: String): DeviceConfiguration? {
        val client = activeClients[deviceId] ?: return null
        
        return try {
            val config = client.readConfiguration()
            DeviceConfiguration(
                deviceId = deviceId,
                switchSetup = config["switchSetup"] ?: 0,
                solarVoltageCalib = config["solarVCalib"] ?: 1000,
                solarCurrentCalib = config["solarICalib"] ?: 1000,
                powerOutVoltageCalib = config["powerOutVCalib"] ?: 1000,
                batteryVoltageCalib = config["batteryVCalib"] ?: 1000,
                chargeCurrentCalib = config["chargeICalib"] ?: 1000,
                batteryCurrentCalib = config["batteryICalib"] ?: 1000,
                internalTempCalib = config["intTempCalib"] ?: 1000,
                buckCurrentCalib = config["buckICalib"] ?: 1000
            )
        } catch (e: Exception) {
            println("Error reading device configuration: ${e.message}")
            null
        }
    }
    
    override suspend fun writeDeviceConfiguration(
        deviceId: String,
        config: DeviceConfiguration
    ): Boolean {
        val client = activeClients[deviceId] ?: return false
        
        return try {
            // Write each configuration value
            // Address mapping based on your SOLPOT_HOLDING_REG_ID_4.mbp
            client.writeConfigurationValue(0, config.switchSetup) &&
            client.writeConfigurationValue(1, config.solarVoltageCalib) &&
            client.writeConfigurationValue(2, config.solarCurrentCalib) &&
            client.writeConfigurationValue(3, config.powerOutVoltageCalib) &&
            client.writeConfigurationValue(4, config.batteryVoltageCalib) &&
            client.writeConfigurationValue(5, config.chargeCurrentCalib) &&
            client.writeConfigurationValue(6, config.batteryCurrentCalib) &&
            client.writeConfigurationValue(7, config.internalTempCalib) &&
            client.writeConfigurationValue(8, config.buckCurrentCalib)
        } catch (e: Exception) {
            println("Error writing device configuration: ${e.message}")
            false
        }
    }
    
    override suspend fun syncToCloud(deviceId: String, data: SolarPanelData): Boolean {
        // TODO: Implement Firebase Firestore sync
        // This will be implemented in platform-specific code
        return true
    }
    
    override suspend fun getHistoricalData(
        deviceId: String,
        startTime: Long,
        endTime: Long
    ): List<SolarPanelData> {
        // TODO: Implement Firebase Firestore query
        return emptyList()
    }
    
    fun cleanup() {
        scope.cancel()
        pollingJobs.values.forEach { it.cancel() }
        pollingJobs.clear()
        
        activeClients.values.forEach { client ->
            scope.launch { client.disconnect() }
        }
        activeClients.clear()
        dataStreams.clear()
    }
}
