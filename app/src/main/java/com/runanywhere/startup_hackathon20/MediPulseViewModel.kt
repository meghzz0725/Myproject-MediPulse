package com.runanywhere.startup_hackathon20

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import com.runanywhere.sdk.models.ModelInfo
import com.runanywhere.startup_hackathon20.agents.MasterOrchestratorAgent
import com.runanywhere.startup_hackathon20.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MediPulseViewModel(application: Application) : AndroidViewModel(application) {

    // Master Orchestrator - initialize with delay to allow SDK setup
    private var _orchestrator: MasterOrchestratorAgent? = null
    val orchestrator: MasterOrchestratorAgent
        get() = _orchestrator ?: throw IllegalStateException("Orchestrator not initialized")

    // UI State
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _statusMessage = MutableStateFlow("Initializing MediPulse...")
    val statusMessage: StateFlow<String> = _statusMessage

    // Model management
    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress

    private val _currentModelId = MutableStateFlow<String?>(null)
    val currentModelId: StateFlow<String?> = _currentModelId

    // Emergency data - lazy initialization
    val activeEmergencies by lazy { _orchestrator?.activeEmergencies ?: MutableStateFlow(emptyList()) }
    val orchestrationLog by lazy { _orchestrator?.orchestrationLog ?: MutableStateFlow(emptyList()) }

    private val _lastResponse = MutableStateFlow<String>("")
    val lastResponse: StateFlow<String> = _lastResponse

    init {
        initializeWithDelay(application)
    }

    private fun initializeWithDelay(application: Application) {
        viewModelScope.launch {
            try {
                // Give SDK time to initialize
                delay(500)
                _orchestrator = MasterOrchestratorAgent(application.applicationContext)
                loadAvailableModels()
            } catch (e: Exception) {
                _statusMessage.value = "Initialization error: ${e.message}. Retrying..."
                delay(1000)
                try {
                    _orchestrator = MasterOrchestratorAgent(application.applicationContext)
                    loadAvailableModels()
                } catch (e2: Exception) {
                    _statusMessage.value = "SDK not ready. App will work with limited functionality."
                }
            }
        }
    }

    // Tab management
    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    // Model management
    private fun loadAvailableModels() {
        viewModelScope.launch {
            try {
                val models = listAvailableModels()
                _availableModels.value = models
                _statusMessage.value = if (models.isEmpty()) {
                    "No models available. Please check SDK initialization."
                } else {
                    "MediPulse ready. Please download and load a model to enable AI features."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading models: ${e.message}"
            }
        }
    }

    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Downloading model..."
                RunAnywhere.downloadModel(modelId).collect { progress ->
                    _downloadProgress.value = progress
                    _statusMessage.value = "Downloading: ${(progress * 100).toInt()}%"
                }
                _downloadProgress.value = null
                _statusMessage.value = "Download complete! Please load the model."
                loadAvailableModels()
            } catch (e: Exception) {
                _statusMessage.value = "Download failed: ${e.message}"
                _downloadProgress.value = null
            }
        }
    }

    fun loadModel(modelId: String) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Loading model..."
                val success = RunAnywhere.loadModel(modelId)
                if (success) {
                    _currentModelId.value = modelId
                    _statusMessage.value = "✅ AI Model loaded! MediPulse fully operational."
                } else {
                    _statusMessage.value = "Failed to load model"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading model: ${e.message}"
            }
        }
    }

    fun refreshModels() {
        loadAvailableModels()
    }

    // Emergency Operations

    /**
     * Report an accident emergency
     */
    fun reportAccident(location: Location, description: String) {
        if (_orchestrator == null) {
            _statusMessage.value = "System still initializing, please wait..."
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "🚨 Processing accident report..."

            try {
                val response = orchestrator.acciAidAgent.reportAccident(location, description)
                if (response.success) {
                    val event = response.data as EmergencyEvent
                    val orchestratorResponse = orchestrator.handleEmergency(event)
                    _lastResponse.value = orchestratorResponse.message
                    _statusMessage.value = "✅ " + orchestratorResponse.message
                } else {
                    _statusMessage.value = "❌ Failed: ${response.message}"
                }
            } catch (e: Exception) {
                _statusMessage.value = "❌ Error: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    /**
     * Request blood emergency
     */
    fun requestBloodEmergency(
        bloodType: String,
        patientName: String,
        location: Location,
        urgency: EmergencyPriority
    ) {
        if (_orchestrator == null) {
            _statusMessage.value = "System still initializing, please wait..."
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "🩸 Processing blood request..."

            try {
                val event = EmergencyEvent(
                    id = "BLOOD-${System.currentTimeMillis()}",
                    type = EmergencyType.BLOOD_EMERGENCY,
                    priority = urgency,
                    location = location,
                    description = "Blood emergency: $bloodType needed for $patientName",
                    patientInfo = PatientInfo(name = patientName, bloodType = bloodType)
                )

                val response = orchestrator.handleEmergency(event)
                _lastResponse.value = response.message
                _statusMessage.value =
                    if (response.success) "✅ ${response.message}" else "❌ ${response.message}"
            } catch (e: Exception) {
                _statusMessage.value = "❌ Error: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    /**
     * Request maternity emergency
     */
    fun requestMaternityEmergency(
        patientName: String,
        age: Int,
        location: Location,
        description: String
    ) {
        if (_orchestrator == null) {
            _statusMessage.value = "System still initializing, please wait..."
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "👶 Processing maternity emergency..."

            try {
                val event = EmergencyEvent(
                    id = "MAT-${System.currentTimeMillis()}",
                    type = EmergencyType.MATERNITY,
                    priority = EmergencyPriority.HIGH,
                    location = location,
                    description = description,
                    patientInfo = PatientInfo(name = patientName, age = age)
                )

                val response = orchestrator.handleEmergency(event)
                _lastResponse.value = response.message
                _statusMessage.value =
                    if (response.success) "✅ ${response.message}" else "❌ ${response.message}"
            } catch (e: Exception) {
                _statusMessage.value = "❌ Error: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    /**
     * Start accident monitoring
     */
    fun startAccidentMonitoring() {
        _orchestrator?.startAccidentMonitoring()
        _statusMessage.value = "🔍 Accident monitoring active"
    }

    /**
     * Stop accident monitoring
     */
    fun stopAccidentMonitoring() {
        _orchestrator?.stopAccidentMonitoring()
        _statusMessage.value = "⏸️ Accident monitoring paused"
    }

    /**
     * Get all hospitals
     */
    fun getAllHospitals() = _orchestrator?.acciAidAgent?.getAllHospitals() ?: emptyList()

    /**
     * Get all available ambulances
     */
    fun getAvailableAmbulances() = _orchestrator?.acciAidAgent?.getAvailableAmbulances() ?: emptyList()

    /**
     * Get blood inventory
     */
    fun getAllBloodInventories() = _orchestrator?.lifeLinkAgent?.getAllBloodInventories() ?: emptyMap()

    /**
     * Get traffic conditions
     */
    fun getTrafficConditions() = _orchestrator?.routeIQAgent?.getTrafficConditions() ?: emptyMap()

    /**
     * Clear orchestration log
     */
    fun clearLog() {
        _orchestrator?.clearLog()
    }

    /**
     * Simulate an accident for testing
     */
    fun simulateAccident() {
        viewModelScope.launch {
            _statusMessage.value = "🧪 Simulating accident..."
            reportAccident(
                location = Location(17.4065, 78.4772, "Gachibowli, Hyderabad"),
                description = "Simulated vehicle collision detected by sensors"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        _orchestrator?.stopAccidentMonitoring()
    }
}
