package com.runanywhere.startup_hackathon20.agents

import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.startup_hackathon20.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * LifeLink Agent - Manages blood and maternity emergencies
 *
 * Features:
 * - Blood bank management and matching
 * - Real-time blood availability tracking
 * - Maternity emergency coordination
 * - Hospital API integration
 * - Donor network management
 */
class LifeLinkAgent {

    private val _bloodRequests = MutableStateFlow<List<BloodRequest>>(emptyList())
    val bloodRequests: StateFlow<List<BloodRequest>> = _bloodRequests

    private val _maternityEmergencies = MutableStateFlow<List<EmergencyEvent>>(emptyList())
    val maternityEmergencies: StateFlow<List<EmergencyEvent>> = _maternityEmergencies

    // Mock blood bank database
    private val bloodBankInventory = mutableMapOf(
        "H001" to mapOf(
            "A+" to 15, "A-" to 5, "B+" to 12, "B-" to 3,
            "O+" to 20, "O-" to 8, "AB+" to 7, "AB-" to 2
        ),
        "H002" to mapOf(
            "A+" to 25, "A-" to 10, "B+" to 18, "B-" to 6,
            "O+" to 30, "O-" to 12, "AB+" to 10, "AB-" to 4
        ),
        "H003" to mapOf(
            "A+" to 8, "A-" to 3, "B+" to 10, "B-" to 2,
            "O+" to 15, "O-" to 5, "AB+" to 4, "AB-" to 1
        ),
        "H004" to mapOf(
            "A+" to 18, "A-" to 7, "B+" to 14, "B-" to 5,
            "O+" to 22, "O-" to 9, "AB+" to 8, "AB-" to 3
        )
    )

    // Mock donor network
    private val registeredDonors = mutableListOf(
        Donor(
            "D001",
            "Rajesh Kumar",
            "O+",
            Location(17.4100, 78.4800, "Gachibowli"),
            "+91-9876543220",
            true
        ),
        Donor(
            "D002",
            "Priya Sharma",
            "A+",
            Location(17.4239, 78.4738, "Jubilee Hills"),
            "+91-9876543221",
            true
        ),
        Donor(
            "D003",
            "Vikram Singh",
            "B+",
            Location(17.4435, 78.3772, "Madhapur"),
            "+91-9876543222",
            true
        ),
        Donor(
            "D004",
            "Anjali Reddy",
            "AB+",
            Location(17.3850, 78.4867, "Malakpet"),
            "+91-9876543223",
            true
        ),
        Donor(
            "D005",
            "Karthik Rao",
            "O-",
            Location(17.4200, 78.4600, "HITEC City"),
            "+91-9876543224",
            true
        )
    )

    /**
     * Request blood from hospital blood banks
     */
    suspend fun requestBlood(
        bloodType: String,
        units: Int,
        patientName: String,
        location: Location,
        urgency: EmergencyPriority,
        hospitals: List<Hospital>
    ): AgentResponse {
        return try {
            Log.i("LifeLinkAgent", "Blood request: $bloodType, $units units for $patientName")

            // Find hospitals with required blood type
            val availableHospitals = hospitals.filter { hospital ->
                val inventory = bloodBankInventory[hospital.id]
                inventory != null && (inventory[bloodType] ?: 0) >= units && hospital.hasBloodBank
            }

            if (availableHospitals.isNotEmpty()) {
                // Select nearest hospital
                val selectedHospital = availableHospitals.minByOrNull { hospital ->
                    calculateDistance(location, hospital.location)
                }!!

                // Create blood request
                val request = BloodRequest(
                    id = "BR-${System.currentTimeMillis()}",
                    bloodType = bloodType,
                    units = units,
                    urgency = urgency,
                    patientName = patientName,
                    hospital = selectedHospital,
                    status = "APPROVED"
                )

                // Update inventory
                val currentInventory = bloodBankInventory[selectedHospital.id]!!.toMutableMap()
                currentInventory[bloodType] = currentInventory[bloodType]!! - units
                bloodBankInventory[selectedHospital.id] = currentInventory

                _bloodRequests.value = _bloodRequests.value + request

                // Use AI to generate guidance
                val aiPrompt = """
                    Blood emergency: Patient needs $units units of $bloodType blood.
                    Available at ${selectedHospital.name}.
                    Priority: $urgency
                    
                    Provide brief instructions for hospital staff and transport team.
                """.trimIndent()

                var aiGuidance = ""
                RunAnywhere.generateStream(aiPrompt).collect { token ->
                    aiGuidance += token
                }

                AgentResponse(
                    agentName = "LifeLink",
                    success = true,
                    message = "Blood available at ${selectedHospital.name}. $units units of $bloodType reserved.",
                    data = request
                )
            } else {
                // Try to find donors
                val donorsResponse = findNearbyDonors(bloodType, location, units)

                if (donorsResponse.success) {
                    AgentResponse(
                        agentName = "LifeLink",
                        success = true,
                        message = "Blood not available in banks. Found ${(donorsResponse.data as List<*>).size} nearby donors.",
                        data = donorsResponse.data
                    )
                } else {
                    AgentResponse(
                        agentName = "LifeLink",
                        success = false,
                        message = "Blood type $bloodType not available in sufficient quantity. Expanding search..."
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("LifeLinkAgent", "Error processing blood request", e)
            AgentResponse(
                agentName = "LifeLink",
                success = false,
                message = "Failed to process blood request: ${e.message}"
            )
        }
    }

    /**
     * Find nearby blood donors
     */
    private fun findNearbyDonors(
        bloodType: String,
        location: Location,
        unitsNeeded: Int
    ): AgentResponse {
        val compatibleDonors = registeredDonors
            .filter { it.bloodType == bloodType && it.isAvailable }
            .map { donor ->
                donor.copy(
                    distance = calculateDistance(location, donor.location)
                )
            }
            .sortedBy { it.distance }
            .take(unitsNeeded)

        return if (compatibleDonors.isNotEmpty()) {
            AgentResponse(
                agentName = "LifeLink",
                success = true,
                message = "Found ${compatibleDonors.size} compatible donors nearby",
                data = compatibleDonors
            )
        } else {
            AgentResponse(
                agentName = "LifeLink",
                success = false,
                message = "No compatible donors found nearby"
            )
        }
    }

    /**
     * Handle maternity emergency
     */
    suspend fun handleMaternityEmergency(
        patientInfo: PatientInfo,
        location: Location,
        description: String,
        hospitals: List<Hospital>
    ): AgentResponse {
        return try {
            Log.i("LifeLinkAgent", "Maternity emergency for ${patientInfo.name}")

            // Find hospitals with maternity ward
            val maternityHospitals = hospitals.filter {
                it.hasMaternityWard && it.availableBeds > 0
            }.sortedBy {
                calculateDistance(location, it.location)
            }

            if (maternityHospitals.isNotEmpty()) {
                val selectedHospital = maternityHospitals.first()

                val event = EmergencyEvent(
                    id = "MAT-${System.currentTimeMillis()}",
                    type = EmergencyType.MATERNITY,
                    priority = EmergencyPriority.HIGH,
                    location = location,
                    description = description,
                    patientInfo = patientInfo,
                    status = EmergencyStatus.DISPATCHED
                )

                _maternityEmergencies.value = _maternityEmergencies.value + event

                // Use AI to provide medical guidance
                val aiPrompt = """
                    Maternity emergency: ${description}
                    Patient: ${patientInfo.name}, Age: ${patientInfo.age}
                    Destination: ${selectedHospital.name}
                    Distance: ${
                    String.format(
                        "%.1f",
                        calculateDistance(location, selectedHospital.location)
                    )
                } km
                    
                    Provide immediate care instructions for paramedics during transport.
                """.trimIndent()

                var aiGuidance = ""
                RunAnywhere.generateStream(aiPrompt).collect { token ->
                    aiGuidance += token
                }

                AgentResponse(
                    agentName = "LifeLink",
                    success = true,
                    message = "Maternity ward secured at ${selectedHospital.name}. Bed reserved.",
                    data = mapOf(
                        "event" to event,
                        "hospital" to selectedHospital,
                        "guidance" to aiGuidance
                    )
                )
            } else {
                AgentResponse(
                    agentName = "LifeLink",
                    success = false,
                    message = "No maternity wards available nearby"
                )
            }
        } catch (e: Exception) {
            Log.e("LifeLinkAgent", "Error handling maternity emergency", e)
            AgentResponse(
                agentName = "LifeLink",
                success = false,
                message = "Failed to process maternity emergency: ${e.message}"
            )
        }
    }

    /**
     * Get blood inventory for a hospital
     */
    fun getBloodInventory(hospitalId: String): Map<String, Int>? {
        return bloodBankInventory[hospitalId]
    }

    /**
     * Get all blood inventories
     */
    fun getAllBloodInventories(): Map<String, Map<String, Int>> {
        return bloodBankInventory.toMap()
    }

    /**
     * Get available donors by blood type
     */
    fun getAvailableDonors(bloodType: String? = null): List<Donor> {
        return if (bloodType != null) {
            registeredDonors.filter { it.bloodType == bloodType && it.isAvailable }
        } else {
            registeredDonors.filter { it.isAvailable }
        }
    }

    /**
     * Register new donor
     */
    fun registerDonor(donor: Donor): AgentResponse {
        return try {
            registeredDonors.add(donor)
            AgentResponse(
                agentName = "LifeLink",
                success = true,
                message = "Donor ${donor.name} registered successfully"
            )
        } catch (e: Exception) {
            AgentResponse(
                agentName = "LifeLink",
                success = false,
                message = "Failed to register donor: ${e.message}"
            )
        }
    }

    private fun calculateDistance(loc1: Location, loc2: Location): Double {
        val earthRadius = 6371.0
        val lat1Rad = Math.toRadians(loc1.latitude)
        val lat2Rad = Math.toRadians(loc2.latitude)
        val deltaLat = Math.toRadians(loc2.latitude - loc1.latitude)
        val deltaLon = Math.toRadians(loc2.longitude - loc1.longitude)

        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}

// Donor data class
data class Donor(
    val id: String,
    val name: String,
    val bloodType: String,
    val location: Location,
    val contact: String,
    val isAvailable: Boolean,
    val distance: Double = 0.0
)
