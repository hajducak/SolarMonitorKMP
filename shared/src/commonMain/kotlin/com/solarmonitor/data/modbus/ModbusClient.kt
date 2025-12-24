package com.solarmonitor.data.modbus

import com.solarmonitor.domain.models.DeviceInfo
import com.solarmonitor.domain.models.SolarPanelData
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock

/**
 * Modbus TCP/RTU client for communicating with solar monitoring devices
 * Supports encrypted connections via TLS
 */
class ModbusClient(
    private val device: DeviceInfo,
    private val useTLS: Boolean = true,
    private val timeout: Long = 5000L
) {
    private var socket: Socket? = null
    private var readChannel: ByteReadChannel? = null
    private var writeChannel: ByteWriteChannel? = null
    private val selectorManager = SelectorManager(Dispatchers.IO)
    
    /**
     * Connects to the Modbus device
     */
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect() // Close any existing connection

            withTimeout(timeout) {
                socket = aSocket(selectorManager)
                    .tcp()
                    .connect(device.ipAddress, device.port)
                    .let { socket ->
                        if (useTLS) {
                            // Enable TLS for encrypted communication
                            socket.tls(Dispatchers.IO)
                        } else {
                            socket
                        }
                    }

                readChannel = socket?.openReadChannel()
                writeChannel = socket?.openWriteChannel(autoFlush = true)
            }
            
            println("Connected to ${device.address} (TLS: $useTLS)")
            true
        } catch (e: Exception) {
            println("Connection error to ${device.address}: ${e.message}")
            disconnect()
            false
        }
    }
    
    /**
     * Disconnects from the device
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            readChannel?.cancel()
            writeChannel?.close()
            socket?.close()
        } catch (e: Exception) {
            println("Disconnect error: ${e.message}")
        } finally {
            readChannel = null
            writeChannel = null
            socket = null
        }
    }
    
    /**
     * Checks if currently connected
     */
    fun isConnected(): Boolean {
        return socket?.isClosed == false
    }
    
    /**
     * Reads all input registers from the solar device
     * According to your SOLPOT_INPUT_REG_ID_4.mbp configuration
     */
    suspend fun readSolarData(): SolarPanelData = withContext(Dispatchers.IO) {
        if (!isConnected()) {
            throw ModbusException("Not connected to device")
        }
        
        try {
            // Read input registers starting from address 0, quantity 7
            // Based on your device: Solar Current, Solar Voltage, Power Out Voltage,
            // Int Temperature, Solar Temperature, 3V3, Power Out Current
            val registers = readInputRegisters(startAddress = 0, quantity = 7)
            
            if (registers == null || registers.size < 7) {
                throw ModbusException("Invalid response from device")
            }
            
            SolarPanelData(
                deviceId = device.id,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                solarCurrent = ModbusProtocol.registerToFloat(registers[0], 100f),
                solarVoltage = ModbusProtocol.registerToFloat(registers[1], 100f),
                powerOutVoltage = ModbusProtocol.registerToFloat(registers[2], 100f),
                internalTemperature = ModbusProtocol.registerToFloat(registers[3], 100f),
                solarTemperature = ModbusProtocol.registerToFloat(registers[4], 100f),
                voltage3V3 = ModbusProtocol.registerToFloat(registers[5], 1000f),
                powerOutCurrent = ModbusProtocol.registerToFloat(registers[6], 100f)
            )
        } catch (e: Exception) {
            throw ModbusException("Error reading solar data: ${e.message}", e)
        }
    }
    
    /**
     * Reads holding registers for configuration
     */
    suspend fun readConfiguration(): Map<String, Int> = withContext(Dispatchers.IO) {
        if (!isConnected()) {
            throw ModbusException("Not connected to device")
        }
        
        try {
            // Read holding registers for calibration values
            val registers = readHoldingRegisters(startAddress = 0, quantity = 9)
            
            if (registers == null || registers.size < 9) {
                throw ModbusException("Invalid configuration response")
            }
            
            mapOf(
                "switchSetup" to registers[0],
                "solarVCalib" to registers[1],
                "solarICalib" to registers[2],
                "powerOutVCalib" to registers[3],
                "batteryVCalib" to registers[4],
                "chargeICalib" to registers[5],
                "batteryICalib" to registers[6],
                "intTempCalib" to registers[7],
                "buckICalib" to registers[8]
            )
        } catch (e: Exception) {
            throw ModbusException("Error reading configuration: ${e.message}", e)
        }
    }
    
    /**
     * Writes a configuration value to a holding register
     */
    suspend fun writeConfigurationValue(address: Int, value: Int): Boolean = 
        withContext(Dispatchers.IO) {
            if (!isConnected()) {
                throw ModbusException("Not connected to device")
            }
            
            try {
                val request = ModbusProtocol.createWriteSingleRegisterRequest(
                    slaveId = device.slaveId,
                    address = address,
                    value = value
                )
                
                writeChannel?.writeFully(request, 0, request.size)
                
                // Read response
                val response = ByteArray(8)
                withTimeout(timeout) {
                    readChannel?.readFully(response, 0, response.size)
                }
                
                ModbusProtocol.verifyCRC(response)
            } catch (e: Exception) {
                println("Error writing configuration: ${e.message}")
                false
            }
        }
    
    /**
     * Reads input registers (Function 04)
     */
    private suspend fun readInputRegisters(startAddress: Int, quantity: Int): IntArray? {
        val request = ModbusProtocol.createReadInputRegistersRequest(
            slaveId = device.slaveId,
            startAddress = startAddress,
            quantity = quantity
        )
        
        return sendRequest(request, expectedResponseSize = 5 + (quantity * 2))
    }
    
    /**
     * Reads holding registers (Function 03)
     */
    private suspend fun readHoldingRegisters(startAddress: Int, quantity: Int): IntArray? {
        val request = ModbusProtocol.createReadHoldingRegistersRequest(
            slaveId = device.slaveId,
            startAddress = startAddress,
            quantity = quantity
        )
        
        return sendRequest(request, expectedResponseSize = 5 + (quantity * 2))
    }
    
    /**
     * Sends a Modbus request and waits for response
     */
    private suspend fun sendRequest(request: ByteArray, expectedResponseSize: Int): IntArray? {
        return withContext(Dispatchers.IO) {
            try {
                // Send request
                writeChannel?.writeFully(request, 0, request.size)
                
                // Read response with timeout
                val response = ByteArray(expectedResponseSize)
                withTimeout(timeout) {
                    readChannel?.readFully(response, 0, response.size)
                }
                
                // Parse response
                ModbusProtocol.parseRegistersResponse(response)
            } catch (e: TimeoutCancellationException) {
                println("Modbus request timeout")
                null
            } catch (e: Exception) {
                println("Modbus request error: ${e.message}")
                null
            }
        }
    }
}
