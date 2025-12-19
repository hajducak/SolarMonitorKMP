package com.solarmonitor.domain.models

import kotlinx.serialization.Serializable

/**
 * Represents a discovered or configured solar monitoring device
 */
@Serializable
data class DeviceInfo(
    val id: String,                  // Unique device identifier
    val name: String,                // User-friendly name
    val ipAddress: String,           // Device IP address (e.g., "10.10.100.253")
    val port: Int = 8893,           // Modbus TCP port
    val slaveId: Int = 4,           // Modbus slave ID
    val isOnline: Boolean = false,   // Connection status
    val lastSeen: Long = 0L,        // Last successful communication timestamp
    val panelLocation: String = "", // Physical location description
    val firmwareVersion: String = "1.0"
) {
    val address: String get() = "$ipAddress:$port"
}

/**
 * Configuration data for a solar device
 */
@Serializable
data class DeviceConfiguration(
    val deviceId: String,
    val switchSetup: Int = 0,
    
    // Calibration values
    val solarVoltageCalib: Int = 1000,
    val solarCurrentCalib: Int = 1000,
    val powerOutVoltageCalib: Int = 1000,
    val batteryVoltageCalib: Int = 1000,
    val chargeCurrentCalib: Int = 1000,
    val batteryCurrentCalib: Int = 1000,
    val internalTempCalib: Int = 1000,
    val buckCurrentCalib: Int = 1000
)
