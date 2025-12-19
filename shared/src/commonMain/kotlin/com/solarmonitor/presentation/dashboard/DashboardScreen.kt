package com.solarmonitor.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarmonitor.domain.models.DeviceInfo
import com.solarmonitor.domain.models.SolarPanelData

/**
 * Main Dashboard Screen
 * Shows list of devices and real-time solar panel data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDevice = uiState.devices.find { it.id == uiState.selectedDeviceId }
    val selectedDeviceData by viewModel.getDeviceData(uiState.selectedDeviceId ?: "").collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solar Monitor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.discoverDevices() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("🔍", fontSize = 24.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Error message
            uiState.errorMessage?.let { error ->
                ErrorBanner(message = error, onDismiss = { viewModel.clearError() })
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Discovery progress
            if (uiState.isDiscovering) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Discovering devices...")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Device list or selected device view
            if (selectedDevice != null && selectedDeviceData != null) {
                DeviceDetailView(
                    device = selectedDevice,
                    data = selectedDeviceData!!,
                    onBack = { viewModel.selectDevice("") },
                    onDisconnect = { viewModel.disconnectFromDevice(selectedDevice.id) }
                )
            } else {
                DeviceListView(
                    devices = uiState.devices,
                    onDeviceClick = { device ->
                        if (device.isOnline) {
                            viewModel.selectDevice(device.id)
                        } else {
                            viewModel.connectToDevice(device.id)
                        }
                    },
                    onRemoveDevice = { device -> viewModel.removeDevice(device.id) }
                )
            }
        }
    }
}

/**
 * Displays list of available devices
 */
@Composable
fun DeviceListView(
    devices: List<DeviceInfo>,
    onDeviceClick: (DeviceInfo) -> Unit,
    onRemoveDevice: (DeviceInfo) -> Unit
) {
    Text(
        text = "Devices (${devices.size})",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(16.dp))
    
    if (devices.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No devices found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tap 🔍 to discover devices",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(devices) { device ->
                DeviceCard(
                    device = device,
                    onClick = { onDeviceClick(device) },
                    onRemove = { onRemoveDevice(device) }
                )
            }
        }
    }
}

/**
 * Card showing device information
 */
@Composable
fun DeviceCard(
    device: DeviceInfo,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isOnline) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (device.panelLocation.isNotEmpty()) {
                    Text(
                        text = device.panelLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (device.isOnline) Color.Green else Color.Gray,
                        shape = RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

/**
 * Detailed view of a single device with real-time data
 */
@Composable
fun DeviceDetailView(
    device: DeviceInfo,
    data: SolarPanelData,
    onBack: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back")
            }
            
            Text(
                text = device.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Disconnect")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Data display
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Solar Input
            item {
                DataCard(
                    title = "☀️ Solar Input",
                    items = listOf(
                        "Voltage" to "${String.format("%.2f", data.solarVoltage)} V",
                        "Current" to "${String.format("%.2f", data.solarCurrent)} A",
                        "Power" to "${String.format("%.2f", data.solarPower)} W"
                    )
                )
            }
            
            // Power Output
            item {
                DataCard(
                    title = "⚡ Power Output",
                    items = listOf(
                        "Voltage" to "${String.format("%.2f", data.powerOutVoltage)} V",
                        "Current" to "${String.format("%.2f", data.powerOutCurrent)} A",
                        "Power" to "${String.format("%.2f", data.outputPower)} W"
                    )
                )
            }
            
            // Temperature
            item {
                DataCard(
                    title = "🌡️ Temperature",
                    items = listOf(
                        "Panel" to "${String.format("%.1f", data.solarTemperature)} °C",
                        "Internal" to "${String.format("%.1f", data.internalTemperature)} °C"
                    )
                )
            }
            
            // System
            item {
                DataCard(
                    title = "🔧 System",
                    items = listOf(
                        "Efficiency" to "${String.format("%.1f", data.efficiency)} %",
                        "3.3V Rail" to "${String.format("%.3f", data.voltage3V3)} V"
                    )
                )
            }
        }
    }
}

/**
 * Card displaying data points
 */
@Composable
fun DataCard(
    title: String,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                if (items.last() != (label to value)) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Error banner component
 */
@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}
