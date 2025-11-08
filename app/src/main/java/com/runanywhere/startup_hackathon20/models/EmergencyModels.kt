package com.runanywhere.startup_hackathon20.models

import kotlinx.serialization.Serializable

// Emergency Types
enum class EmergencyType {
    ACCIDENT,
    BLOOD_EMERGENCY,
    MATERNITY,
    GENERAL
}

enum class EmergencyPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

enum class EmergencyStatus {
    DETECTED,
    DISPATCHED,
    EN_ROUTE,
    ARRIVED,
    RESOLVED,
    CANCELLED
}

// Location Data
data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)

// Emergency Event
data class EmergencyEvent(
    val id: String,
    val type: EmergencyType,
    val priority: EmergencyPriority,
    val location: Location,
    val timestamp: Long = System.currentTimeMillis(),
    val status: EmergencyStatus = EmergencyStatus.DETECTED,
    val description: String,
    val sensorData: SensorData? = null,
    val patientInfo: PatientInfo? = null
)

// Sensor Data for AcciAid
data class SensorData(
    val accelerometerX: Float,
    val accelerometerY: Float,
    val accelerometerZ: Float,
    val gyroscopeX: Float,
    val gyroscopeY: Float,
    val gyroscopeZ: Float,
    val impactForce: Float,
    val isCollisionDetected: Boolean
)

// Patient Information
data class PatientInfo(
    val name: String? = null,
    val age: Int? = null,
    val bloodType: String? = null,
    val medicalConditions: List<String> = emptyList(),
    val emergencyContact: String? = null
)

// Hospital Data
data class Hospital(
    val id: String,
    val name: String,
    val location: Location,
    val phone: String,
    val hasEmergencyWard: Boolean,
    val hasBloodBank: Boolean,
    val hasMaternityWard: Boolean,
    val availableBeds: Int,
    val distance: Double = 0.0,
    val estimatedTime: Int = 0 // in minutes
)

// Ambulance Data
data class Ambulance(
    val id: String,
    val vehicleNumber: String,
    val currentLocation: Location,
    val isAvailable: Boolean,
    val assignedHospital: String,
    val driverName: String,
    val driverContact: String
)

// Blood Request
data class BloodRequest(
    val id: String,
    val bloodType: String,
    val units: Int,
    val urgency: EmergencyPriority,
    val patientName: String,
    val hospital: Hospital,
    val status: String = "PENDING"
)

// Route Data
data class Route(
    val origin: Location,
    val destination: Location,
    val distance: Double, // in kilometers
    val duration: Int, // in minutes
    val trafficLevel: TrafficLevel,
    val waypoints: List<Location> = emptyList(),
    val instructions: List<String> = emptyList()
)

enum class TrafficLevel {
    CLEAR,
    LIGHT,
    MODERATE,
    HEAVY,
    BLOCKED
}

// Agent Response
data class AgentResponse(
    val agentName: String,
    val success: Boolean,
    val message: String,
    val data: Any? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// Dispatch Result
data class DispatchResult(
    val emergencyId: String,
    val ambulance: Ambulance,
    val hospital: Hospital,
    val route: Route,
    val estimatedArrival: Int
)
