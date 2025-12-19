package com.solarmonitor.domain.models

import kotlinx.serialization.Serializable

/**
 * Domain model representing real-time data from a solar panel
 */
@Serializable
data class SolarPanelData(
    val deviceId: String,
    val timestamp: Long,
    
    // Current readings
    val solarVoltage: Float,        // in Volts
    val solarCurrent: Float,        // in Amperes
    val powerOutVoltage: Float,     // in Volts
    val powerOutCurrent: Float,     // in Amperes
    val internalTemperature: Float, // in Celsius
    val solarTemperature: Float,    // in Celsius
    val voltage3V3: Float           // 3.3V rail voltage
) {
    // Calculated values
    val solarPower: Float get() = solarVoltage * solarCurrent
    val outputPower: Float get() = powerOutVoltage * powerOutCurrent
    val efficiency: Float get() = if (solarPower > 0) (outputPower / solarPower) * 100 else 0f
    companion object {
        fun empty(deviceId: String) = SolarPanelData(
            deviceId = deviceId,
            timestamp = 0L,
            solarVoltage = 0f,
            solarCurrent = 0f,
            powerOutVoltage = 0f,
            powerOutCurrent = 0f,
            internalTemperature = 0f,
            solarTemperature = 0f,
            voltage3V3 = 0f
        )
    }
}
