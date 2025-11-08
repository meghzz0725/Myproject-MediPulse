package com.runanywhere.startup_hackathon20.agents

import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.startup_hackathon20.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * RouteIQ Agent - Finds fastest ambulance routes using real-time traffic
 *
 * Features:
 * - Real-time traffic monitoring
 * - Dynamic route optimization
 * - Alternative route suggestions
 * - ETA calculations with traffic patterns
 * - Road closure detection
 */
class RouteIQAgent {

    // Simulated traffic data for different areas
    private val trafficConditions = mutableMapOf(
        "Gachibowli" to TrafficLevel.MODERATE,
        "Jubilee Hills" to TrafficLevel.LIGHT,
        "Madhapur" to TrafficLevel.HEAVY,
        "Malakpet" to TrafficLevel.MODERATE,
        "HITEC City" to TrafficLevel.HEAVY,
        "Banjara Hills" to TrafficLevel.LIGHT,
        "Kukatpally" to TrafficLevel.MODERATE,
        "Secunderabad" to TrafficLevel.HEAVY
    )

    /**
     * Calculate optimal route with real-time traffic
     */
    suspend fun calculateOptimalRoute(
        origin: Location,
        destination: Location,
        priority: EmergencyPriority
    ): AgentResponse {
        return try {
            Log.i(
                "RouteIQAgent",
                "Calculating route from ${origin.address} to ${destination.address}"
            )

            // Calculate base distance
            val distance = calculateDistance(origin, destination)

            // Determine traffic level
            val trafficLevel = getTrafficLevel(destination.address ?: "")

            // Calculate duration based on traffic
            val baseDuration = (distance / 40 * 60).toInt() // 40 km/h base speed
            val trafficMultiplier = when (trafficLevel) {
                TrafficLevel.CLEAR -> 0.8
                TrafficLevel.LIGHT -> 1.0
                TrafficLevel.MODERATE -> 1.3
                TrafficLevel.HEAVY -> 1.7
                TrafficLevel.BLOCKED -> 2.5
            }

            // Priority emergencies get faster routes
            val priorityMultiplier = when (priority) {
                EmergencyPriority.CRITICAL -> 0.7  // Use sirens, traffic clearing
                EmergencyPriority.HIGH -> 0.85
                EmergencyPriority.MEDIUM -> 1.0
                EmergencyPriority.LOW -> 1.1
            }

            val finalDuration = (baseDuration * trafficMultiplier * priorityMultiplier).toInt()

            // Generate waypoints
            val waypoints = generateWaypoints(origin, destination, trafficLevel)

            // Generate turn-by-turn instructions using AI
            val aiPrompt = """
                Generate emergency route instructions:
                From: ${origin.address}
                To: ${destination.address}
                Distance: ${String.format("%.1f", distance)} km
                Traffic: $trafficLevel
                Priority: $priority
                
                Provide 3-4 key turn-by-turn navigation instructions for ambulance driver.
            """.trimIndent()

            var aiInstructions = ""
            RunAnywhere.generateStream(aiPrompt).collect { token ->
                aiInstructions += token
            }

            val instructions = parseInstructions(aiInstructions)

            val route = Route(
                origin = origin,
                destination = destination,
                distance = distance,
                duration = finalDuration,
                trafficLevel = trafficLevel,
                waypoints = waypoints,
                instructions = instructions
            )

            AgentResponse(
                agentName = "RouteIQ",
                success = true,
                message = "Optimal route calculated. ETA: $finalDuration minutes. Traffic: $trafficLevel",
                data = route
            )
        } catch (e: Exception) {
            Log.e("RouteIQAgent", "Error calculating route", e)
            AgentResponse(
                agentName = "RouteIQ",
                success = false,
                message = "Failed to calculate route: ${e.message}"
            )
        }
    }

    /**
     * Get alternative routes
     */
    suspend fun getAlternativeRoutes(
        origin: Location,
        destination: Location,
        priority: EmergencyPriority
    ): AgentResponse {
        return try {
            val routes = mutableListOf<Route>()

            // Generate 3 alternative routes with different characteristics
            for (i in 1..3) {
                val distance = calculateDistance(origin, destination) * (0.9 + i * 0.15)
                val trafficLevel = when (i) {
                    1 -> TrafficLevel.LIGHT
                    2 -> TrafficLevel.MODERATE
                    else -> TrafficLevel.HEAVY
                }

                val duration = (distance / 40 * 60 * getTrafficMultiplier(trafficLevel)).toInt()

                routes.add(
                    Route(
                        origin = origin,
                        destination = destination,
                        distance = distance,
                        duration = duration,
                        trafficLevel = trafficLevel,
                        waypoints = generateWaypoints(origin, destination, trafficLevel),
                        instructions = listOf(
                            "Route $i: ${if (i == 1) "Fastest route via highway" else if (i == 2) "Balanced route via main roads" else "Alternative route via side streets"}",
                            "Distance: ${String.format("%.1f", distance)} km",
                            "ETA: $duration minutes"
                        )
                    )
                )
            }

            AgentResponse(
                agentName = "RouteIQ",
                success = true,
                message = "Found ${routes.size} alternative routes",
                data = routes
            )
        } catch (e: Exception) {
            AgentResponse(
                agentName = "RouteIQ",
                success = false,
                message = "Failed to get alternative routes: ${e.message}"
            )
        }
    }

    /**
     * Monitor route in real-time and provide updates
     */
    fun monitorRoute(route: Route): Flow<RouteUpdate> = flow {
        var currentProgress = 0
        var currentTraffic = route.trafficLevel

        while (currentProgress < 100) {
            delay(3000) // Update every 3 seconds

            currentProgress += Random.nextInt(5, 15)
            if (currentProgress > 100) currentProgress = 100

            // Randomly change traffic conditions
            if (Random.nextFloat() < 0.2) {
                currentTraffic = TrafficLevel.values()[Random.nextInt(TrafficLevel.values().size)]
            }

            val update = RouteUpdate(
                progress = currentProgress,
                remainingDistance = route.distance * (100 - currentProgress) / 100,
                remainingTime = (route.duration * (100 - currentProgress) / 100).toInt(),
                currentTraffic = currentTraffic,
                alert = if (currentTraffic == TrafficLevel.HEAVY || currentTraffic == TrafficLevel.BLOCKED)
                    "Heavy traffic ahead. Consider alternate route." else null
            )

            emit(update)
        }
    }

    /**
     * Report traffic incident
     */
    suspend fun reportTrafficIncident(
        location: Location,
        incidentType: String,
        severity: String
    ): AgentResponse {
        return try {
            Log.i("RouteIQAgent", "Traffic incident reported: $incidentType at ${location.address}")

            // Update traffic conditions
            location.address?.let { address ->
                val area = extractArea(address)
                trafficConditions[area] = when (severity.uppercase()) {
                    "HIGH", "SEVERE" -> TrafficLevel.BLOCKED
                    "MEDIUM" -> TrafficLevel.HEAVY
                    else -> TrafficLevel.MODERATE
                }
            }

            AgentResponse(
                agentName = "RouteIQ",
                success = true,
                message = "Traffic incident reported and routes updated"
            )
        } catch (e: Exception) {
            AgentResponse(
                agentName = "RouteIQ",
                success = false,
                message = "Failed to report incident: ${e.message}"
            )
        }
    }

    /**
     * Get current traffic conditions
     */
    fun getTrafficConditions(): Map<String, TrafficLevel> {
        return trafficConditions.toMap()
    }

    /**
     * Predict traffic based on time of day
     */
    fun predictTraffic(area: String, hour: Int): TrafficLevel {
        return when (hour) {
            in 7..10 -> TrafficLevel.HEAVY  // Morning rush
            in 11..16 -> TrafficLevel.MODERATE
            in 17..20 -> TrafficLevel.HEAVY  // Evening rush
            in 21..23, in 0..6 -> TrafficLevel.LIGHT  // Night time
            else -> TrafficLevel.MODERATE
        }
    }

    private fun getTrafficLevel(address: String): TrafficLevel {
        val area = extractArea(address)
        return trafficConditions[area] ?: TrafficLevel.MODERATE
    }

    private fun getTrafficMultiplier(trafficLevel: TrafficLevel): Double {
        return when (trafficLevel) {
            TrafficLevel.CLEAR -> 0.8
            TrafficLevel.LIGHT -> 1.0
            TrafficLevel.MODERATE -> 1.3
            TrafficLevel.HEAVY -> 1.7
            TrafficLevel.BLOCKED -> 2.5
        }
    }

    private fun extractArea(address: String): String {
        // Extract area name from address
        val areas = listOf(
            "Gachibowli", "Jubilee Hills", "Madhapur", "Malakpet",
            "HITEC City", "Banjara Hills", "Kukatpally", "Secunderabad"
        )
        return areas.firstOrNull { address.contains(it, ignoreCase = true) } ?: "Unknown"
    }

    private fun generateWaypoints(
        origin: Location,
        destination: Location,
        traffic: TrafficLevel
    ): List<Location> {
        val waypoints = mutableListOf<Location>()
        val steps = if (traffic == TrafficLevel.HEAVY) 4 else 3

        for (i in 1 until steps) {
            val fraction = i.toDouble() / steps
            val lat = origin.latitude + (destination.latitude - origin.latitude) * fraction
            val lon = origin.longitude + (destination.longitude - origin.longitude) * fraction
            waypoints.add(Location(lat, lon, "Waypoint $i"))
        }

        return waypoints
    }

    private fun parseInstructions(aiText: String): List<String> {
        // Parse AI-generated instructions
        return aiText.lines()
            .filter {
                it.isNotBlank() && (it.contains("turn", ignoreCase = true) ||
                        it.contains("continue", ignoreCase = true) ||
                        it.contains("proceed", ignoreCase = true) ||
                        it.contains("take", ignoreCase = true))
            }
            .take(5)
            .ifEmpty {
                listOf(
                    "Proceed to destination via optimal route",
                    "Follow GPS navigation",
                    "Use sirens for emergency clearance"
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

// Route update data class for real-time monitoring
data class RouteUpdate(
    val progress: Int,  // Percentage 0-100
    val remainingDistance: Double,  // in km
    val remainingTime: Int,  // in minutes
    val currentTraffic: TrafficLevel,
    val alert: String? = null
)
