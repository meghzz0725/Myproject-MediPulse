package com.runanywhere.startup_hackathon20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.models.*
import com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Startup_hackathon20Theme {
                MediPulseApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediPulseApp(viewModel: MediPulseViewModel = viewModel()) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeEmergencies by viewModel.activeEmergencies.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("MediPulse", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Emergency Response System",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Dashboard") },
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("AcciAid") },
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                    label = { Text("LifeLink") },
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    label = { Text("RouteIQ") },
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Setup") },
                    selected = selectedTab == 4,
                    onClick = { viewModel.selectTab(4) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status Bar
            StatusBar(statusMessage, isLoading, activeEmergencies.size)

            // Content based on selected tab
            when (selectedTab) {
                0 -> DashboardScreen(viewModel)
                1 -> AcciAidScreen(viewModel)
                2 -> LifeLinkScreen(viewModel)
                3 -> RouteIQScreen(viewModel)
                4 -> SetupScreen(viewModel)
            }
        }
    }
}

@Composable
fun StatusBar(message: String, isLoading: Boolean, activeEmergencyCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            if (activeEmergencyCount > 0) {
                Badge {
                    Text("$activeEmergencyCount")
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: MediPulseViewModel) {
    val activeEmergencies by viewModel.activeEmergencies.collectAsState()
    val orchestrationLog by viewModel.orchestrationLog.collectAsState()
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Actions
        item {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionCard(
                    title = "Simulate\nAccident",
                    icon = Icons.Filled.Person,
                    color = Color(0xFFE91E63),
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.simulateAccident()
                }
                QuickActionCard(
                    title = "Start\nMonitoring",
                    icon = Icons.Filled.Phone,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.startAccidentMonitoring()
                }
            }
        }

        // Active Emergencies
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Active Emergencies",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${activeEmergencies.size} active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (activeEmergencies.isEmpty()) {
            item {
                EmptyStateCard("No active emergencies", Icons.Filled.CheckCircle)
            }
        } else {
            items(activeEmergencies) { emergency ->
                EmergencyCard(emergency)
            }
        }

        // Orchestration Log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Activity Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.clearLog() }) {
                    Text("Clear")
                }
            }
        }

        if (orchestrationLog.isEmpty()) {
            item {
                EmptyStateCard("No activity yet", Icons.Filled.Info)
            }
        } else {
            items(orchestrationLog.takeLast(20).reversed()) { logEntry ->
                LogEntryCard(logEntry)
            }
        }
    }

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(orchestrationLog.size) {
        if (orchestrationLog.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
}

@Composable
fun AcciAidScreen(viewModel: MediPulseViewModel) {
    var location by remember { mutableStateOf("Gachibowli, Hyderabad") }
    var description by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AgentHeaderCard(
                title = "AcciAid Agent",
                subtitle = "Accident Detection & Ambulance Dispatch",
                icon = Icons.Filled.Person,
                color = Color(0xFFE91E63)
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Report Accident", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (location.isNotBlank()) {
                                viewModel.reportAccident(
                                    Location(17.4065, 78.4772, location),
                                    description.ifBlank { "Accident reported by user" }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Report Accident")
                    }
                }
            }
        }

        item {
            Text("Nearby Hospitals", style = MaterialTheme.typography.titleMedium)
        }

        items(viewModel.getAllHospitals().take(4)) { hospital ->
            HospitalCard(hospital)
        }

        item {
            Text("Available Ambulances", style = MaterialTheme.typography.titleMedium)
        }

        items(viewModel.getAvailableAmbulances()) { ambulance ->
            AmbulanceCard(ambulance)
        }
    }
}

@Composable
fun LifeLinkScreen(viewModel: MediPulseViewModel) {
    var patientName by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("O+") }
    var location by remember { mutableStateOf("Gachibowli, Hyderabad") }
    var selectedEmergency by remember { mutableStateOf("Blood") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AgentHeaderCard(
                title = "LifeLink Agent",
                subtitle = "Blood & Maternity Emergency Management",
                icon = Icons.Filled.Favorite,
                color = Color(0xFFF44336)
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = selectedEmergency == "Blood",
                            onClick = { selectedEmergency = "Blood" },
                            label = { Text("Blood Emergency") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = selectedEmergency == "Maternity",
                            onClick = { selectedEmergency = "Maternity" },
                            label = { Text("Maternity") }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (selectedEmergency == "Blood") {
                        BloodEmergencyForm(viewModel, patientName, bloodType, location,
                            onPatientNameChange = { patientName = it },
                            onBloodTypeChange = { bloodType = it },
                            onLocationChange = { location = it }
                        )
                    } else {
                        MaternityEmergencyForm(viewModel, patientName, location,
                            onPatientNameChange = { patientName = it },
                            onLocationChange = { location = it }
                        )
                    }
                }
            }
        }

        item {
            Text("Blood Bank Inventory", style = MaterialTheme.typography.titleMedium)
        }

        val inventories = viewModel.getAllBloodInventories()
        item {
            BloodInventoryGrid(inventories)
        }
    }
}

@Composable
fun RouteIQScreen(viewModel: MediPulseViewModel) {
    val trafficConditions = viewModel.getTrafficConditions()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AgentHeaderCard(
                title = "RouteIQ Agent",
                subtitle = "Real-time Traffic & Route Optimization",
                icon = Icons.Filled.Phone,
                color = Color(0xFF4CAF50)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Real-time Traffic Monitoring",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "AI-powered route optimization active",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Live Traffic Conditions", style = MaterialTheme.typography.titleMedium)
        }

        items(trafficConditions.entries.toList()) { (area, traffic) ->
            TrafficCard(area, traffic)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Route Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    FeatureRow(Icons.Filled.Phone, "Dynamic route recalculation")
                    FeatureRow(Icons.Filled.Phone, "Real-time traffic updates")
                    FeatureRow(Icons.Filled.Star, "ETA prediction with AI")
                    FeatureRow(Icons.Filled.Star, "Incident detection & avoidance")
                }
            }
        }
    }
}

@Composable
fun SetupScreen(viewModel: MediPulseViewModel) {
    val availableModels by viewModel.availableModels.collectAsState()
    val currentModelId by viewModel.currentModelId.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AgentHeaderCard(
                title = "AI Model Setup",
                subtitle = "Configure on-device AI for emergency intelligence",
                icon = Icons.Filled.Settings,
                color = Color(0xFF9C27B0)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "⚡ On-Device AI Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Emergency severity assessment\n" +
                                "• Intelligent dispatch recommendations\n" +
                                "• Route navigation instructions\n" +
                                "• Medical guidance generation",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Available Models", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { viewModel.refreshModels() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Refresh")
                }
            }
        }

        downloadProgress?.let { progress ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Downloading... ${(progress * 100).toInt()}%")
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (availableModels.isEmpty()) {
            item {
                EmptyStateCard("Initializing models...", Icons.Filled.Add)
            }
        } else {
            items(availableModels) { model ->
                ModelCard(
                    model = model,
                    isLoaded = model.id == currentModelId,
                    onDownload = { viewModel.downloadModel(model.id) },
                    onLoad = { viewModel.loadModel(model.id) }
                )
            }
        }
    }
}

// UI Components

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = color
            )
        }
    }
}

@Composable
fun AgentHeaderCard(title: String, subtitle: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmergencyCard(emergency: EmergencyEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (emergency.priority) {
                EmergencyPriority.CRITICAL -> Color(0xFFE91E63).copy(alpha = 0.1f)
                EmergencyPriority.HIGH -> Color(0xFFFF9800).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    emergency.type.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Badge {
                    Text(emergency.status.toString())
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                emergency.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Row {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    emergency.location.address ?: "Location Unknown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LogEntryCard(logEntry: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            logEntry,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@Composable
fun EmptyStateCard(message: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HospitalCard(hospital: Hospital) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(hospital.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row {
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(hospital.location.address ?: "", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hospital.hasEmergencyWard) Chip("Emergency")
                if (hospital.hasBloodBank) Chip("Blood Bank")
                if (hospital.hasMaternityWard) Chip("Maternity")
            }
            Text("Available Beds: ${hospital.availableBeds}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AmbulanceCard(ambulance: Ambulance) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(ambulance.vehicleNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Driver: ${ambulance.driverName}", style = MaterialTheme.typography.bodySmall)
                Text(ambulance.driverContact, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun BloodEmergencyForm(
    viewModel: MediPulseViewModel,
    patientName: String,
    bloodType: String,
    location: String,
    onPatientNameChange: (String) -> Unit,
    onBloodTypeChange: (String) -> Unit,
    onLocationChange: (String) -> Unit
) {
    OutlinedTextField(
        value = patientName,
        onValueChange = onPatientNameChange,
        label = { Text("Patient Name") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))

    // Blood type selector
    Text("Blood Type", style = MaterialTheme.typography.labelMedium)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-").forEach { type ->
            FilterChip(
                selected = bloodType == type,
                onClick = { onBloodTypeChange(type) },
                label = { Text(type) }
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = location,
        onValueChange = onLocationChange,
        label = { Text("Location") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(12.dp))

    Button(
        onClick = {
            if (patientName.isNotBlank()) {
                viewModel.requestBloodEmergency(
                    bloodType,
                    patientName,
                    Location(17.4065, 78.4772, location),
                    EmergencyPriority.HIGH
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Filled.Favorite, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Request Blood")
    }
}

@Composable
fun MaternityEmergencyForm(
    viewModel: MediPulseViewModel,
    patientName: String,
    location: String,
    onPatientNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit
) {
    var age by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    OutlinedTextField(
        value = patientName,
        onValueChange = onPatientNameChange,
        label = { Text("Patient Name") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = age,
        onValueChange = { age = it },
        label = { Text("Age") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = location,
        onValueChange = onLocationChange,
        label = { Text("Location") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Description") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )

    Spacer(Modifier.height(12.dp))

    Button(
        onClick = {
            if (patientName.isNotBlank()) {
                viewModel.requestMaternityEmergency(
                    patientName,
                    age.toIntOrNull() ?: 30,
                    Location(17.4065, 78.4772, location),
                    description.ifBlank { "Maternity emergency" }
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Filled.Favorite, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Request Maternity Care")
    }
}

@Composable
fun BloodInventoryGrid(inventories: Map<String, Map<String, Int>>) {
    inventories.forEach { (hospitalId, inventory) ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Hospital $hospitalId", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    inventory.forEach { (bloodType, units) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(bloodType, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("$units", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun TrafficCard(area: String, traffic: TrafficLevel) {
    val color = when (traffic) {
        TrafficLevel.CLEAR -> Color(0xFF4CAF50)
        TrafficLevel.LIGHT -> Color(0xFF8BC34A)
        TrafficLevel.MODERATE -> Color(0xFFFF9800)
        TrafficLevel.HEAVY -> Color(0xFFFF5722)
        TrafficLevel.BLOCKED -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(area, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(traffic.toString(), style = MaterialTheme.typography.bodyMedium, color = color)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Phone,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun FeatureRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ModelCard(
    model: com.runanywhere.sdk.models.ModelInfo,
    isLoaded: Boolean,
    onDownload: () -> Unit,
    onLoad: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLoaded)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                model.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isLoaded) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Currently Active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f),
                        enabled = !model.isDownloaded
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (model.isDownloaded) "Downloaded" else "Download")
                    }

                    Button(
                        onClick = onLoad,
                        modifier = Modifier.weight(1f),
                        enabled = model.isDownloaded
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Load")
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}