package com.example.gpstracker.server.reports

import com.example.gpstracker.model.api.NewReportRequest
import com.example.gpstracker.model.api.VoteRequest
import com.example.gpstracker.model.geo.LatLon
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

/**
 * - `GET  /api/reports?lat=..&lon=..`   active reports nearby; anyone, also in demo mode
 * - `POST /api/reports`                  share a report (signed in)
 * - `POST /api/reports/{id}/vote`        answer "still there?" (signed in, once per report)
 */
fun Route.roadReports(reports: ReportService, tokenAuth: String) {
    route("/reports") {
        authenticate(tokenAuth, optional = true) {
            get {
                val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
                val lon = call.request.queryParameters["lon"]?.toDoubleOrNull()
                val near = if (lat != null && lon != null) LatLon(lat, lon) else null
                call.respond(reports.active(call.accountId(), near))
            }
        }
        authenticate(tokenAuth) {
            post {
                call.respond(HttpStatusCode.Created, reports.create(call.receive<NewReportRequest>(), call.accountId()!!))
            }
            post("/{id}/vote") {
                val id = call.parameters["id"].orEmpty()
                call.respond(reports.vote(id, call.accountId()!!, call.receive<VoteRequest>().stillThere))
            }
        }
    }
}

private fun RoutingCall.accountId(): UUID? = principal<JWTPrincipal>()?.payload?.subject?.let(UUID::fromString)
