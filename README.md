# Solar Monitor KMP

A Kotlin Multiplatform Mobile (KMP) application for monitoring and managing solar panel systems with encrypted Modbus TCP communication.

## 📱 Features

### MVP (Current Version)
- ✅ **Device Discovery**: Automatic discovery of solar monitoring devices on local network
- ✅ **Real-time Monitoring**: Live data display from solar panels
  - Solar voltage & current
  - Power output
  - Panel & internal temperature
  - System efficiency
- ✅ **Multi-device Support**: Monitor up to 4+ solar panels simultaneously
- ✅ **Encrypted Communication**: TLS/SSL support for secure Modbus TCP connections
- ✅ **Device Configuration**: Read and write calibration values

### Roadmap
- 🔄 Firebase Integration for cloud data storage
- 🔄 Historical data visualization with charts
- 🔄 Push notifications for alerts
- 🔄 iOS app deployment

## 🏗️ Architecture

```
SolarMonitorKMP/
├── shared/                          # Shared KMP module
│   ├── src/
│   │   ├── commonMain/             # Platform-agnostic code
│   │   │   ├── kotlin/com/solarmonitor/
│   │   │   │   ├── data/
│   │   │   │   │   ├── modbus/    # Modbus protocol implementation
│   │   │   │   │   │   ├── ModbusProtocol.kt
│   │   │   │   │   │   └── ModbusClient.kt
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── SolarDeviceRepositoryImpl.kt
│   │   │   │   │   └── models/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── models/    # Domain entities
│   │   │   │   │   │   ├── SolarPanelData.kt
│   │   │   │   │   │   └── DeviceInfo.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       └── SolarDeviceRepository.kt
│   │   │   │   └── presentation/  # ViewModels & UI
│   │   │   │       └── dashboard/
│   │   │   │           ├── DashboardViewModel.kt
│   │   │   │           └── DashboardScreen.kt
│   │   ├── androidMain/           # Android-specific code
│   │   └── iosMain/               # iOS-specific code
│   └── build.gradle.kts
├── androidApp/                     # Android application
│   ├── src/main/
│   │   ├── kotlin/com/solarmonitor/android/
│   │   │   └── MainActivity.kt
│   │   ├── AndroidManifest.xml
│   │   └── res/
│   └── build.gradle.kts
├── iosApp/                        # iOS application (future)
├── build.gradle.kts
└── settings.gradle.kts
```

## 🔧 Hardware Specifications

### Solar Monitoring Device
- **Microcontroller**: STM32F072C8U6
- **Communication**: Elfin-EW11 WiFi-to-RS485 bridge
- **Protocol**: Modbus RTU over TCP/IP
- **Default Configuration**:
  - IP Address: `10.10.100.253`
  - Port: `8893`
  - Slave ID: `4`
  - Baud Rate: `19200`

### Monitored Parameters
#### Input Registers (Read-Only)
- Solar Current (A)
- Solar Voltage (V)
- Power Output Voltage (V)
- Power Output Current (A)
- Internal Temperature (°C)
- Solar Panel Temperature (°C)
- 3.3V Rail Voltage

#### Holding Registers (Configuration)
- Switch Setup
- Voltage/Current Calibration values
- Temperature Calibration values

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17** or later
- **Android SDK** with minimum API 24
- **Kotlin** 1.9.21
- **Gradle** 8.1+

### Setup Instructions

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/SolarMonitorKMP.git
cd SolarMonitorKMP
```

2. **Configure your network**
   - Ensure your Android device/emulator is on the same network as your solar devices
   - Default IP range for discovery: `10.10.100.250-254`
   - Update IP range in `SolarDeviceRepositoryImpl.kt` if needed:
     ```kotlin
     private val ipBase = "10.10.100"
     private val ipRange = 250..254
     ```

3. **Build and Run**
```bash
# For Android
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug

# Or open in Android Studio and run
```

4. **Configure Firebase (Optional)**
   - Create a Firebase project at https://console.firebase.google.com
   - Download `google-services.json`
   - Place it in `androidApp/` directory
   - Uncomment Firebase sync code in repository

## 📖 Usage Guide

### First Time Setup

1. **Launch the app** on your Android device

2. **Discover Devices**
   - Tap the 🔍 floating action button
   - Wait for device discovery to complete
   - Found devices will appear in the list

3. **Connect to Device**
   - Tap on any discovered device
   - App will establish encrypted TLS connection
   - Real-time data will start streaming

4. **Monitor Data**
   - View live solar input (voltage, current, power)
   - Monitor power output
   - Check temperatures
   - View system efficiency

### Managing Multiple Devices

The app supports monitoring multiple solar panels:
- Each device appears in the list
- Online devices show green indicator
- Tap any device to view its detailed data
- Swipe to disconnect or remove devices

## 🔐 Security

### Encrypted Communication
- **TLS 1.2** encryption for all Modbus TCP connections
- Protects data in transit over WiFi
- Can be toggled in `ModbusClient` initialization:
  ```kotlin
  ModbusClient(device, useTLS = true)
  ```

### Network Security
- App uses `android:usesCleartextTraffic="true"` for local network communication
- Consider implementing certificate pinning for production
- Add authentication for cloud sync

## 🧪 Testing

### Manual Device Addition
If auto-discovery doesn't work:
1. Use the manual add device feature (to be implemented in UI)
2. Or modify code to add devices directly:
```kotlin
viewModel.addDeviceManually(
    name = "My Solar Panel",
    ipAddress = "10.10.100.253",
    port = 8893,
    slaveId = 4
)
```

### Testing Without Hardware
- Mock data generation (to be implemented)
- Modbus simulator tools
- Use Modbus Poll on Windows for testing protocol

## 📚 Technical Details

### Modbus Protocol Implementation

#### CRC16 Calculation
The app implements proper Modbus RTU CRC16 checksum calculation:
```kotlin
fun calculateCRC16(data: ByteArray, length: Int): Int
```

#### Supported Functions
- `0x03`: Read Holding Registers
- `0x04`: Read Input Registers  
- `0x06`: Write Single Register
- `0x10`: Write Multiple Registers (future)

### Data Flow

```
Device (STM32) → Elfin-EW11 → WiFi → Android App
                  RS485        TCP      Ktor Client
                                        ModbusClient
```

## 🐛 Troubleshooting

### Device Not Discovered
- Verify device is powered on
- Check WiFi connection
- Ensure device IP is in scan range
- Confirm firewall isn't blocking port 8893

### Connection Failed
- Check TLS configuration
- Verify Modbus slave ID (default: 4)
- Test with Modbus Poll first
- Review logs in Android Studio Logcat

### Data Reading Errors
- Confirm register addresses match device configuration
- Check CRC calculation
- Verify data scaling factors (typically /100 or /1000)

## 🤝 Contributing

This is a personal project for your friend's solar monitoring system. Feel free to:
- Report issues
- Suggest features
- Submit pull requests

## 📄 License

[Specify your license here]

## 🙏 Acknowledgments

- STM32 hardware by your friend
- Elfin-EW11 WiFi bridge by Hi-Flying
- Kotlin Multiplatform by JetBrains
- Ktor networking library

## 📞 Support

For issues specific to:
- **Hardware**: Contact device manufacturer
- **App**: Create GitHub issue
- **Network**: Check router/AP configuration

---

**Version**: 1.0.0 (MVP)  
**Last Updated**: December 2024  
**Platform**: Android (iOS coming soon)
