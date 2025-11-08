package com.runanywhere.startup_hackathon20.agents

import android.content.Context
import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.startup_hackathon20.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Master Orchestrator Agent - Coordinates all emergency response sub-agents
 *
 * This is the central brain of MediPulse that:
 * 1. Receives emergency events from AcciAid, LifeLink, or direct reports
 * 2. Analyzes the situation using AI
 * 3. Coordinates appropriate sub-agents (AcciAid, LifeLink, RouteIQ)
 * 4. Manages the entire emergency response workflow
 * 5. Provides real-time updates and AI-powered decision support
 */
class MasterOrchestratorAgent(private val context: Context) {

    // Initialize all sub-agents
    val acciAidAgent = AcciAidAgent(context)
    val lifeLinkAgent = LifeLinkAgent()
    val routeIQAgent = RouteIQAgent()

    private val _activeEmergencies = MutableStateFlow<List<EmergencyEvent>>(emptyList())
    val activeEmergencies: StateFlow<List<EmergencyEvent>> = _activeEmergencies

    private val _orchestrationLog = MutableStateFlow<List<String>>(emptyList())
    val orchestrationLog: StateFlow<List<String>> = _orchestrationLog

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Monitor AcciAid for accident detections
        scope.launch {
            acciAidAgent.accidentDetected.collect { event ->
                event?.let {
                    log("⚠️ ACCIDENT DETECTED by AcciAid Agent")
                    handleEmergency(it)
                }
            }
        }
    }

    /**
     * Main orchestration method - handles any emergency event
     */
    suspend fun handleEmergency(event: EmergencyEvent): OrchestratorResponse {
        log("🧠 Master Orchestrator: Processing ${event.type} emergency (ID: ${event.id})")

        // Add to active emergencies
        _activeEmergencies.value = _activeEmergencies.value + event

        return when (event.type) {
            EmergencyType.ACCIDENT -> handleAccidentEmergency(event)
            EmergencyType.BLOOD_EMERGENCY -> handleBloodEmergency(event)
            EmergencyType.MATERNITY -> handleMaternityEmergency(event)
            EmergencyType.GENERAL -> handleGeneralEmergency(event)
        }
    }

    /**
     * Handle accident emergencies - coordinates AcciAid and RouteIQ
     */
    private suspend fun handleAccidentEmergency(event: EmergencyEvent): OrchestratorResponse {
        log("🚑 Orchestrating accident response...")

        val responses = mutableListOf<AgentResponse>()

        try {
            // Step 1: Use AI to assess severity
            val aiAssessment = assessEmergencySeverity(event)
            log("AI Assessment: $aiAssessment")

            // Step 2: Find nearest hospitals via AcciAid
            log("📍 AcciAid: Finding nearest hospitals...")
            val hospitals = acciAidAgent.findNearestHospitals(event.location, 3)
            log("Found ${hospitals.size} nearby hospitals")

            if (hospitals.isEmpty()) {
                return OrchestratorResponse(
                    success = false,
                    message = "No hospitals found nearby",
                    agentResponses = responses
                )
            }

            val selectedHospital = hospitals.first()
            log(
                "Selected: ${selectedHospital.name} (${
                    String.format(
                        "%.1f",
                        selectedHospital.distance
                    )
                } km)"
            )

            // Step 3: Calculate optimal route via RouteIQ
            log("🛣️ RouteIQ: Calculating optimal route...")
            val routeResponse = routeIQAgent.calculateOptimalRoute(
                event.location,
                selectedHospital.location,
                event.priority
            )
            responses.add(routeResponse)

            if (routeResponse.success) {
                val route = routeResponse.data as Route
                log("Route calculated: ${route.distance} km, ETA: ${route.duration} min, Traffic: ${route.trafficLevel}")
            }

            // Step 4: Dispatch ambulance via AcciAid
            log("🚨 AcciAid: Dispatching ambulance...")
            val dispatchResponse = acciAidAgent.dispatchAmbulance(event, selectedHospital)
            responses.add(dispatchResponse)

            if (dispatchResponse.success) {
                val dispatchResult = dispatchResponse.data as DispatchResult
                log("✅ Ambulance ${dispatchResult.ambulance.vehicleNumber} dispatched")
                log("Driver: ${dispatchResult.ambulance.driverName} - ${dispatchResult.ambulance.driverContact}")

                // Update emergency status
                updateEmergencyStatus(event.id, EmergencyStatus.DISPATCHED)

                return OrchestratorResponse(
                    success = true,
                    message = "Emergency response coordinated successfully. Ambulance en route, ETA: ${dispatchResult.estimatedArrival} minutes",
                    agentResponses = responses,
                    dispatchResult = dispatchResult
                )
            } else {
                return OrchestratorResponse(
                    success = false,
                    message = "Failed to dispatch ambulance: ${dispatchResponse.message}",
                    agentResponses = responses
                )
            }

        } catch (e: Exception) {
            log("❌ Error in orchestration: ${e.message}")
            return OrchestratorResponse(
                success = false,
                message = "Orchestration failed: ${e.message}",
                agentResponses = responses
            )
        }
    }

    /**
     * Handle blood emergencies - coordinates LifeLink and RouteIQ
     */
    private suspend fun handleBloodEmergency(event: EmergencyEvent): OrchestratorResponse {
        log("🩸 Orchestrating blood emergency response...")

        val responses = mutableListOf<AgentResponse>()

        try {
            val patientInfo = event.patientInfo ?: return OrchestratorResponse(
                success = false,
                message = "Patient information required for blood emergency",
                agentResponses = responses
            )

            val bloodType = patientInfo.bloodType ?: return OrchestratorResponse(
                success = false,
                message = "Blood type required",
                agentResponses = responses
            )

            // Step 1: Request blood via LifeLink
            log("🩸 LifeLink: Processing blood request for $bloodType...")
            val hospitals = acciAidAgent.getAllHospitals()
            val bloodResponse = lifeLinkAgent.requestBlood(
                bloodType = bloodType,
                units = 2, // Default 2 units
                patientName = patientInfo.name ?: "Unknown",
                location = event.location,
                urgency = event.priority,
                hospitals = hospitals
            )
            responses.add(bloodResponse)

            if (bloodResponse.success) {
                val bloodRequest = bloodResponse.data as? BloodRequest
                bloodRequest?.let {
                    log("✅ Blood secured at ${it.hospital.name}")

                    // Step 2: Calculate route to hospital
                    val routeResponse = routeIQAgent.calculateOptimalRoute(
                        event.location,
                        it.hospital.location,
                        event.priority
                    )
                    responses.add(routeResponse)

                    updateEmergencyStatus(event.id, EmergencyStatus.DISPATCHED)

                    return OrchestratorResponse(
                        success = true,
                        message = "Blood emergency coordinated. ${it.units} units of $bloodType available at ${it.hospital.name}",
                        agentResponses = responses
                    )
                }
            }

            return OrchestratorResponse(
                success = false,
                message = bloodResponse.message,
                agentResponses = responses
            )

        } catch (e: Exception) {
            log("❌ Error in blood emergency orchestration: ${e.message}")
            return OrchestratorResponse(
                success = false,
                message = "Failed: ${e.message}",
                agentResponses = responses
            )
        }
    }

    /**
     * Handle maternity emergencies - coordinates LifeLink and RouteIQ
     */
    private suspend fun handleMaternityEmergency(event: EmergencyEvent): OrchestratorResponse {
        log("👶 Orchestrating maternity emergency response...")

        val responses = mutableListOf<AgentResponse>()

        try {
            val patientInfo = event.patientInfo ?: PatientInfo(name = "Patient")

            // Step 1: Handle maternity emergency via LifeLink
            log("👶 LifeLink: Finding maternity ward...")
            val hospitals = acciAidAgent.getAllHospitals()
            val maternityResponse = lifeLinkAgent.handleMaternityEmergency(
                patientInfo = patientInfo,
                location = event.location,
                description = event.description,
                hospitals = hospitals
            )
            responses.add(maternityResponse)

            if (maternityResponse.success) {
                val data = maternityResponse.data as Map<*, *>
                val hospital = data["hospital"] as Hospital
                log("✅ Maternity ward secured at ${hospital.name}")

                // Step 2: Dispatch ambulance
                val dispatchResponse = acciAidAgent.dispatchAmbulance(event, hospital)
                responses.add(dispatchResponse)

                updateEmergencyStatus(event.id, EmergencyStatus.DISPATCHED)

                return OrchestratorResponse(
                    success = true,
                    message = "Maternity emergency coordinated. Ambulance dispatched to ${hospital.name}",
                    agentResponses = responses
                )
            }

            return OrchestratorResponse(
                success = false,
                message = maternityResponse.message,
                agentResponses = responses
            )

        } catch (e: Exception) {
            log("❌ Error in maternity emergency orchestration: ${e.message}")
            return OrchestratorResponse(
                success = false,
                message = "Failed: ${e.message}",
                agentResponses = responses
            )
        }
    }

    /**
     * Handle general emergencies
     */
    private suspend fun handleGeneralEmergency(event: EmergencyEvent): OrchestratorResponse {
        log("🏥 Orchestrating general emergency response...")
        return handleAccidentEmergency(event) // Use same flow as accident
    }

    /**
     * Use AI to assess emergency severity
     */
    private suspend fun assessEmergencySeverity(event: EmergencyEvent): String {
        val prompt = """
            Emergency Type: ${event.type}
            Priority: ${event.priority}
            Description: ${event.description}
            ${event.sensorData?.let { "Impact Force: ${it.impactForce} m/s²" } ?: ""}
            
            Provide a brief 1-2 sentence severity assessment and immediate action recommendation.
        """.trimIndent()

        var assessment = ""
        try {
            RunAnywhere.generateStream(prompt).collect { token ->
                assessment += token
            }
        } catch (e: Exception) {
            assessment = "AI assessment unavailable. Proceeding with standard protocol."
        }

        return assessment
    }

    /**
     * Get comprehensive emergency status report
     */
    suspend fun getEmergencyStatusReport(emergencyId: String): String {
        val emergency = _activeEmergencies.value.find { it.id == emergencyId }
            ?: return "Emergency not found"

        val prompt = """
            Generate a status report for emergency:
            ID: ${emergency.id}
            Type: ${emergency.type}
            Status: ${emergency.status}
            Location: ${emergency.location.address}
            Time: ${emergency.timestamp}
            
            Provide a brief status summary.
        """.trimIndent()

        var report = ""
        try {
            RunAnywhere.generateStream(prompt).collect { token ->
                report += token
            }
        } catch (e: Exception) {
            report = "Status: ${emergency.status}"
        }

        return report
    }

    /**
     * Start accident monitoring
     */
    fun startAccidentMonitoring() {
        acciAidAgent.startMonitoring()
        log("🔍 Accident monitoring started")
    }

    /**
     * Stop accident monitoring
     */
    fun stopAccidentMonitoring() {
        acciAidAgent.stopMonitoring()
        log("⏸️ Accident monitoring stopped")
    }

    private fun updateEmergencyStatus(emergencyId: String, status: EmergencyStatus) {
        _activeEmergencies.value = _activeEmergencies.value.map { emergency ->
            if (emergency.id == emergencyId) {
                emergency.copy(status = status)
            } else {
                emergency
            }
        }
        log("📊 Emergency $emergencyId status updated to $status")
    }

    private fun log(message: String) {
        Log.i("MasterOrchestrator", message)
        _orchestrationLog.value =
            _orchestrationLog.value + "[${System.currentTimeMillis() % 100000}] $message"
    }

    fun clearLog() {
        _orchestrationLog.value = emptyList()
    }
}

/**
 * Orchestrator response containing results from all coordinated agents
 */
data class OrchestratorResponse(
    val success: Boolean,
    val message: String,
    val agentResponses: List<AgentResponse>,
    val dispatchResult: DispatchResult? = null
)
