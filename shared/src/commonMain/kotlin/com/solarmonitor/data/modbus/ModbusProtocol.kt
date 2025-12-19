package com.solarmonitor.data.modbus

import kotlin.experimental.and

/**
 * Modbus RTU/TCP protocol implementation
 */
object ModbusProtocol {
    
    // Function codes
    const val READ_HOLDING_REGISTERS = 0x03
    const val READ_INPUT_REGISTERS = 0x04
    const val WRITE_SINGLE_REGISTER = 0x06
    const val WRITE_MULTIPLE_REGISTERS = 0x10
    
    /**
     * Creates a Modbus request to read input registers (Function 04)
     * Based on your device configuration
     */
    fun createReadInputRegistersRequest(
        slaveId: Int,
        startAddress: Int,
        quantity: Int
    ): ByteArray {
        val request = ByteArray(8)
        request[0] = slaveId.toByte()
        request[1] = READ_INPUT_REGISTERS.toByte()
        request[2] = (startAddress shr 8).toByte()  // Address high byte
        request[3] = startAddress.toByte()           // Address low byte
        request[4] = (quantity shr 8).toByte()       // Quantity high byte
        request[5] = quantity.toByte()               // Quantity low byte
        
        val crc = calculateCRC16(request, 6)
        request[6] = (crc and 0xFF).toByte()         // CRC low byte
        request[7] = (crc shr 8).toByte()            // CRC high byte
        
        return request
    }
    
    /**
     * Creates a Modbus request to read holding registers (Function 03)
     */
    fun createReadHoldingRegistersRequest(
        slaveId: Int,
        startAddress: Int,
        quantity: Int
    ): ByteArray {
        val request = ByteArray(8)
        request[0] = slaveId.toByte()
        request[1] = READ_HOLDING_REGISTERS.toByte()
        request[2] = (startAddress shr 8).toByte()
        request[3] = startAddress.toByte()
        request[4] = (quantity shr 8).toByte()
        request[5] = quantity.toByte()
        
        val crc = calculateCRC16(request, 6)
        request[6] = (crc and 0xFF).toByte()
        request[7] = (crc shr 8).toByte()
        
        return request
    }
    
    /**
     * Creates a Modbus request to write a single holding register (Function 06)
     */
    fun createWriteSingleRegisterRequest(
        slaveId: Int,
        address: Int,
        value: Int
    ): ByteArray {
        val request = ByteArray(8)
        request[0] = slaveId.toByte()
        request[1] = WRITE_SINGLE_REGISTER.toByte()
        request[2] = (address shr 8).toByte()
        request[3] = address.toByte()
        request[4] = (value shr 8).toByte()
        request[5] = value.toByte()
        
        val crc = calculateCRC16(request, 6)
        request[6] = (crc and 0xFF).toByte()
        request[7] = (crc shr 8).toByte()
        
        return request
    }
    
    /**
     * Calculates CRC16 checksum for Modbus RTU
     */
    fun calculateCRC16(data: ByteArray, length: Int): Int {
        var crc = 0xFFFF
        
        for (i in 0 until length) {
            crc = crc xor (data[i].toInt() and 0xFF)
            
            for (j in 0 until 8) {
                if ((crc and 0x0001) != 0) {
                    crc = (crc shr 1) xor 0xA001
                } else {
                    crc = crc shr 1
                }
            }
        }
        
        return crc
    }
    
    /**
     * Verifies CRC of received Modbus response
     */
    fun verifyCRC(data: ByteArray): Boolean {
        if (data.size < 4) return false
        
        val receivedCRC = ((data[data.size - 1].toInt() and 0xFF) shl 8) or 
                         (data[data.size - 2].toInt() and 0xFF)
        val calculatedCRC = calculateCRC16(data, data.size - 2)
        
        return receivedCRC == calculatedCRC
    }
    
    /**
     * Parses a Modbus response containing register values
     */
    fun parseRegistersResponse(response: ByteArray): IntArray? {
        if (response.size < 5) return null
        if (!verifyCRC(response)) return null
        
        val byteCount = response[2].toInt() and 0xFF
        val registerCount = byteCount / 2
        val registers = IntArray(registerCount)
        
        for (i in 0 until registerCount) {
            val high = response[3 + i * 2].toInt() and 0xFF
            val low = response[4 + i * 2].toInt() and 0xFF
            registers[i] = (high shl 8) or low
        }
        
        return registers
    }
    
    /**
     * Converts a register value to float (for temperature, voltage, current)
     * Adjusted to your device's scaling
     */
    fun registerToFloat(value: Int, scale: Float = 100f): Float {
        return value.toFloat() / scale
    }
    
    /**
     * Converts float to register value for writing
     */
    fun floatToRegister(value: Float, scale: Float = 100f): Int {
        return (value * scale).toInt()
    }
}

/**
 * Exception thrown when Modbus communication fails
 */
class ModbusException(message: String, cause: Throwable? = null) : Exception(message, cause)
