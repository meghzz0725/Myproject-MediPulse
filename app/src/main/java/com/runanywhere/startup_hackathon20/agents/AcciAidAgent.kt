package com.runanywhere.startup_hackathon20.agents

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location as AndroidLocation
import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.startup_hackathon20.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

/**
 * AcciAid Agent - Detects accidents using sensors and dispatches ambulances
 * 
 * Features:
 * - Real-time accelerometer and gyroscope monitoring
 * - Collision detection algorithm
 * - Nearby hospital database with GPS integration
 * - Automatic ambulance dispatch
 */
class AcciAidAgent(private val context: Context) : SensorEventListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring
    
    private val _accidentDetected = MutableStateFlow<EmergencyEvent?>(null)
    val accidentDetected: StateFlow<EmergencyEvent?> = _accidentDetected
    
    // Sensor data
    private var accelX = 0f
    private var accelY = 0f
    private var accelZ = 0f
    private var gyroX = 0f
    private var gyroY = 0f
    private var gyroZ = 0f
    
    // Collision detection threshold (in m/s²)
    private val COLLISION_THRESHOLD = 30f
    
    // Mock nearby hospitals database
    private val nearbyHospitals = listOf(
        Hospital(
            id = "H001",
            name = "City General Hospital",
            location = Location(17.4065, 78.4772, "Gachibowli, Hyderabad"),
            phone = "+91-9876543210",
            hasEmergencyWard = true,
            hasBloodBank = true,
            hasMaternityWard = true,
            availableBeds = 25
        ),
        Hospital(
            id = "H002",
            name = "Apollo Hospital",
            location = Location(17.4239, 78.4738, "Jubilee Hills, Hyderabad"),
            phone = "+91-9876543211",
            hasEmergencyWard = true,
            hasBloodBank = true,
            hasMaternityWard = true,
            availableBeds = 40
        ),
        Hospital(
            id = "H003",
            name = "Medicover Hospital",
            location = Location(17.4435, 78.3772, "Madhapur, Hyderabad"),
            phone = "+91-9876543212",
            hasEmergencyWard = true,
            hasBloodBank = true,
            hasMaternityWard = false,
            availableBeds = 15
        ),
        Hospital(
            id = "H004",
            name = "Yashoda Hospital",
            location = Location(17.3850, 78.4867, "Malakpet, Hyderabad"),
            phone = "+91-9876543213",
            hasEmergencyWard = true,
            hasBloodBank = true,
            hasMaternityWard = true,
            availableBeds = 30
        )
    )
    
    // Mock available ambulances
    private val availableAmbulances = mutableListOf(
        Ambulance(
            id = "AMB001",
            vehicleNumber = "TS 09 EA 1234",
            currentLocation = Location(17.4100, 78.4800),
            isAvailable = true,
            assignedHospital = "H001",
            driverName = "Ramesh Kumar",
            driverContact = "+91-9123456780"
        ),
        Ambulance(
            id = "AMB002",
            vehicleNumber = "TS 09 EA 5678",
            currentLocation = Location(17.4250, 78.4750),
            isAvailable = true,
            assignedHospital = "H002",
            driverName = "Suresh Reddy",
            driverContact = "+91-9123456781"
        ),
        Ambulance(
            id = "AMB003",
            vehicleNumber = "TS 09 EA 9012",
            currentLocation = Location(17.4400, 78.3800),
            isAvailable = true,
            assignedHospital = "H003",
            driverName = "Mahesh Babu",
            driverContact = "+91-9123456782"
        )
    )
    
    fun startMonitoring() {
        _isMonitoring.value = true
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        Log.i("AcciAidAgent", "Started accident monitoring")
    }
    
    fun stopMonitoring() {
        _isMonitoring.value = false
        sensorManager.unregisterListener(this)
        Log.i("AcciAidAgent", "Stopped accident monitoring")
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accelX = it.values[0]
                    accelY = it.values[1]
                    accelZ = it.values[2]
                    
                    checkForCollision()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroX = it.values[0]
                    gyroY = it.values[1]
                    gyroZ = it.values[2]
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
    
    private fun checkForCollision() {
        val magnitude = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)
        
        if (magnitude > COLLISION_THRESHOLD) {
            detectAccident()
        }
    }
    
    private fun detectAccident() {
        Log.w("AcciAidAgent", "⚠️ ACCIDENT DETECTED!")
        
        val sensorData = SensorData(
            accelerometerX = accelX,
            accelerometerY = accelY,
            accelerometerZ = accelZ,
            gyroscopeX = gyroX,
            gyroscopeY = gyroY,
            gyroscopeZ = gyroZ,
            impactForce = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ),
            isCollisionDetected = true
        )
        
        val event = EmergencyEvent(
            id = "ACC-${System.currentTimeMillis()}",
            type = EmergencyType.ACCIDENT,
            priority = EmergencyPriority.CRITICAL,
            location = Location(17.4065, 78.4772, "Current Location"), // Mock location
            description = "Severe collision detected. Impact force: ${sensorData.impactForce} m/s²",
            sensorData = sensorData
        )
        
        _accidentDetected.value = event
    }
    
    /**
     * Manually report an accident
     */
    suspend fun reportAccident(location: Location, description: String): AgentResponse {
        return try {
            val event = EmergencyEvent(
                id = "ACC-${System.currentTimeMillis()}",
                type = EmergencyType.ACCIDENT,
                priority = EmergencyPriority.HIGH,
                location = location,
                description = description
            )
            
            _accidentDetected.value = event
            
            AgentResponse(
                agentName = "AcciAid",
                success = true,
                message = "Accident reported successfully",
                data = event
            )
        } catch (e: Exception) {
            Log.e("AcciAidAgent", "Error reporting accident", e)
            AgentResponse(
                agentName = "AcciAid",
                success = false,
                message = "Failed to report accident: ${e.message}"
            )
        }
    }
    
    /**
     * Find nearest hospitals based on location
     */
    fun findNearestHospitals(location: Location, limit: Int = 3): List<Hospital> {
        return nearbyHospitals
            .map { hospital ->
                val distance = calculateDistance(location, hospital.location)
                hospital.copy(
                    distance = distance,
                    estimatedTime = (distance / 40 * 60).toInt() // Assuming 40 km/h avg speed
                )
            }
            .sortedBy { it.distance }
            .take(limit)
    }
    
    /**
     * Dispatch ambulance to accident location
     */
    suspend fun dispatchAmbulance(
        emergencyEvent: EmergencyEvent,
        hospital: Hospital
    ): AgentResponse {
        return try {
            // Find available ambulance nearest to accident location
            val ambulance = availableAmbulances
                .filter { it.isAvailable && it.assignedHospital == hospital.id }
                .minByOrNull { calculateDistance(it.currentLocation, emergencyEvent.location) }
            
            if (ambulance != null) {
                // Mark ambulance as dispatched
                ambulance.copy(isAvailable = false)
                
                // Use AI to generate dispatch instructions
                val aiPrompt = """
                    Emergency dispatch for ${emergencyEvent.type} at ${emergencyEvent.location.address}.
                    Ambulance ${ambulance.vehicleNumber} dispatched from ${ambulance.currentLocation.address ?: "base"}.
                    Hospital: ${hospital.name}
                    Priority: ${emergencyEvent.priority}
                    
                    Generate brief dispatch instructions for the driver and emergency response team.
                """.trimIndent()
                
                var aiInstructions = ""
                RunAnywhere.generateStream(aiPrompt).collect { token ->
                    aiInstructions += token
                }
                
                val dispatchResult = DispatchResult(
                    emergencyId = emergencyEvent.id,
                    ambulance = ambulance,
                    hospital = hospital,
                    route = Route(
                        origin = ambulance.currentLocation,
                        destination = emergencyEvent.location,
                        distance = calculateDistance(ambulance.currentLocation, emergencyEvent.location),
                        duration = (calculateDistance(ambulance.currentLocation, emergencyEvent.location) / 40 * 60).toInt(),
                        trafficLevel = TrafficLevel.MODERATE
                    ),
                    estimatedArrival = (calculateDistance(ambulance.currentLocation, emergencyEvent.location) / 40 * 60).toInt()
                )
                
                Log.i("AcciAidAgent", "Ambulance dispatched: ${ambulance.vehicleNumber}")
                
                AgentResponse(
                    agentName = "AcciAid",
                    success = true,
                    message = "Ambulance dispatched successfully. ETA: ${dispatchResult.estimatedArrival} minutes",
                    data = dispatchResult
                )
            } else {
                AgentResponse(
                    agentName = "AcciAid",
                    success = false,
                    message = "No available ambulances at the moment"
                )
            }
        } catch (e: Exception) {
            Log.e("AcciAidAgent", "Error dispatching ambulance", e)
            AgentResponse(
                agentName = "AcciAid",
                success = false,
                message = "Failed to dispatch ambulance: ${e.message}"
            )
        }
    }
    
    /**
     * Calculate distance between two locations (Haversine formula)
     */
    private fun calculateDistance(loc1: Location, loc2: Location): Double {
        val earthRadius = 6371.0 // in kilometers
        
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
    
    fun getAvailableAmbulances(): List<Ambulance> = availableAmbulances.filter { it.isAvailable }
    
    fun getAllHospitals(): List<Hospital> = nearbyHospitals
}
