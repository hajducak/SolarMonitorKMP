package com.solarmonitor.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarmonitor.data.repository.SolarDeviceRepositoryImpl
import com.solarmonitor.domain.models.DeviceInfo
import com.solarmonitor.domain.models.SolarPanelData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the main dashboard screen
 * Manages device discovery, connection, and data display
 */
class DashboardViewModel : ViewModel() {
    
    private val repository = SolarDeviceRepositoryImpl()
    
    // UI State
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    // Device data streams
    private val deviceDataStreams = mutableMapOf<String, StateFlow<SolarPanelData?>>()
    
    init {
        // Observe devices from repository
        viewModelScope.launch {
            repository.getDevices().collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }
    }
    
    /**
     * Discovers devices on the network
     */
    fun discoverDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true, errorMessage = null) }
            
            try {
                val discoveredDevices = repository.discoverDevices()
                
                // Add discovered devices
                discoveredDevices.forEach { device ->
                    repository.addDevice(device)
                }
                
                _uiState.update { 
                    it.copy(
                        isDiscovering = false,
                        errorMessage = if (discoveredDevices.isEmpty()) {
                            "No devices found. Make sure devices are powered on and connected to the network."
                        } else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isDiscovering = false, 
                        errorMessage = "Discovery failed: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Connects to a specific device
     */
    fun connectToDevice(deviceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            
            try {
                val success = repository.connectToDevice(deviceId)
                
                if (success) {
                    // Start collecting data stream
                    startDataStream(deviceId)
                    
                    // Select this device
                    _uiState.update { it.copy(selectedDeviceId = deviceId) }
                } else {
                    _uiState.update { 
                        it.copy(errorMessage = "Failed to connect to device")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Connection error: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Disconnects from a device
     */
    fun disconnectFromDevice(deviceId: String) {
        viewModelScope.launch {
            repository.disconnectFromDevice(deviceId)
            deviceDataStreams.remove(deviceId)
            
            if (_uiState.value.selectedDeviceId == deviceId) {
                _uiState.update { it.copy(selectedDeviceId = null) }
            }
        }
    }
    
    /**
     * Selects a device to view
     */
    fun selectDevice(deviceId: String) {
        _uiState.update { it.copy(selectedDeviceId = deviceId) }
    }
    
    /**
     * Starts collecting data stream from a device
     */
    private fun startDataStream(deviceId: String) {
        val dataStream = repository.getDeviceDataStream(deviceId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
        
        deviceDataStreams[deviceId] = dataStream
    }
    
    /**
     * Gets the data stream for a specific device
     */
    fun getDeviceData(deviceId: String): StateFlow<SolarPanelData?> {
        return deviceDataStreams[deviceId] ?: MutableStateFlow(null)
    }
    
    /**
     * Adds a device manually
     */
    fun addDeviceManually(
        name: String,
        ipAddress: String,
        port: Int = 8893,
        slaveId: Int = 4
    ) {
        viewModelScope.launch {
            val device = DeviceInfo(
                id = "device_${ipAddress.replace(".", "_")}",
                name = name,
                ipAddress = ipAddress,
                port = port,
                slaveId = slaveId
            )
            
            repository.addDevice(device)
        }
    }
    
    /**
     * Removes a device
     */
    fun removeDevice(deviceId: String) {
        viewModelScope.launch {
            repository.removeDevice(deviceId)
            deviceDataStreams.remove(deviceId)
        }
    }
    
    /**
     * Clears error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}

/**
 * UI State for the Dashboard
 */
data class DashboardUiState(
    val devices: List<DeviceInfo> = emptyList(),
    val selectedDeviceId: String? = null,
    val isDiscovering: Boolean = false,
    val errorMessage: String? = null
)
