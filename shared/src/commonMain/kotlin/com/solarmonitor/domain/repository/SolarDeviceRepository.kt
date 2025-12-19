package com.solarmonitor.domain.repository

import com.solarmonitor.domain.models.DeviceConfiguration
import com.solarmonitor.domain.models.DeviceInfo
import com.solarmonitor.domain.models.SolarPanelData
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing solar monitoring devices and their data
 */
interface SolarDeviceRepository {
    
    /**
     * Discovers devices on the local network
     * Scans for devices at known IP addresses
     */
    suspend fun discoverDevices(): List<DeviceInfo>
    
    /**
     * Gets all saved devices
     */
    fun getDevices(): Flow<List<DeviceInfo>>
    
    /**
     * Gets a specific device by ID
     */
    suspend fun getDevice(deviceId: String): DeviceInfo?
    
    /**
     * Adds a new device
     */
    suspend fun addDevice(device: DeviceInfo): Boolean
    
    /**
     * Updates device information
     */
    suspend fun updateDevice(device: DeviceInfo): Boolean
    
    /**
     * Removes a device
     */
    suspend fun removeDevice(deviceId: String): Boolean
    
    /**
     * Connects to a device and starts monitoring
     */
    suspend fun connectToDevice(deviceId: String): Boolean
    
    /**
     * Disconnects from a device
     */
    suspend fun disconnectFromDevice(deviceId: String)
    
    /**
     * Gets real-time data stream from a connected device
     */
    fun getDeviceDataStream(deviceId: String): Flow<SolarPanelData>
    
    /**
     * Reads current data from a device (one-time read)
     */
    suspend fun readDeviceData(deviceId: String): SolarPanelData?
    
    /**
     * Reads device configuration
     */
    suspend fun readDeviceConfiguration(deviceId: String): DeviceConfiguration?
    
    /**
     * Writes device configuration
     */
    suspend fun writeDeviceConfiguration(deviceId: String, config: DeviceConfiguration): Boolean
    
    /**
     * Syncs device data to cloud (Firebase)
     */
    suspend fun syncToCloud(deviceId: String, data: SolarPanelData): Boolean
    
    /**
     * Gets historical data for a device from cloud
     */
    suspend fun getHistoricalData(
        deviceId: String,
        startTime: Long,
        endTime: Long
    ): List<SolarPanelData>
}
