package com.example.gpstracker.server.trips

import com.example.gpstracker.model.api.ApiError
import com.example.gpstracker.model.trip.SavedTrip
import com.example.gpstracker.server.ApiException
import com.example.gpstracker.tracing.Tracing
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

/** Limits of what one driver can store. */
object TripLimits {
    const val MAX_TRIPS_LISTED = 1_000
    const val MAX_ROUTE_POINTS = 20_000
    const val MAX_EVENTS = 1_000
    const val MAX_ID = 64
}

/**
 * - `GET /api/trips`        the signed-in driver's trips, newest first
 * - `PUT /api/trips/{id}`   saves one trip (sending it again replaces it)
 */
fun Route.trips(store: TripStore, tokenAuth: String) {
    val tracer = Tracing.tracer("server")
    authenticate(tokenAuth) {
        route("/trips") {
            get {
                call.respond(store.trips(call.accountId(), TripLimits.MAX_TRIPS_LISTED))
            }
            put("/{id}") {
                val trip = call.receive<SavedTrip>()
                val ok = trip.id == call.parameters["id"] &&
                    trip.id.length <= TripLimits.MAX_ID &&
                    trip.trip.route.size <= TripLimits.MAX_ROUTE_POINTS &&
                    trip.trip.events.size <= TripLimits.MAX_EVENTS &&
                    trip.trip.score in 0..MAX_SCORE
                if (!ok) throw ApiException(HttpStatusCode.BadRequest, ApiError(ApiError.INVALID_INPUT, "This trip cannot be saved."))
                store.save(call.accountId(), trip)
                tracer.info("trip saved for an account", "score" to trip.trip.score)
                call.respond(trip)
            }
        }
    }
}

private const val MAX_SCORE = 100

private fun io.ktor.server.routing.RoutingCall.accountId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)
