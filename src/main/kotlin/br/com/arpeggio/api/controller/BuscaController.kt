package br.com.arpeggio.api.controller

import br.com.arpeggio.api.dto.response.ApiErrorMessageResponse
import br.com.arpeggio.api.dto.response.ResultsResponse
import br.com.arpeggio.api.service.BuscaService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("v1")
@Tag(name = "Search", description = "Endpoints for comparative queries of music releases and podcast episodes across streaming services")
class BuscaController(val service: BuscaService) {

    @Operation(
        summary = "Comparative search for artist releases",
        description = "Queries the number of releases (Albums, Singles, and EPs) for an artist or band across Spotify, Deezer, Tidal, YouTube Music, and Apple Music."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Search completed successfully across all configured streaming services",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ResultsResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters (e.g. blank name or missing required parameters)",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorMessageResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error or invalid region/content type parameter",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorMessageResponse::class))]
            )
        ]
    )
    @GetMapping("/artista")
    suspend fun buscaArtista(
        @Parameter(
            name = "nome",
            description = "Artist or band name to search for",
            example = "Metallica",
            required = true
        )
        @RequestParam(required = true) nome: String,

        @Parameter(
            name = "regiao",
            description = "Country / market code for the query.\n\nAccepted values: `BR`, `US`",
            schema = Schema(type = "string", allowableValues = ["BR", "US"], defaultValue = "BR", example = "BR"),
            required = true
        )
        @RequestParam(required = true) regiao: String,

        @Parameter(
            name = "tipo",
            description = "Comma-separated release types to filter by.\n\nAccepted values: `ALBUM`, `SINGLE`, `EP`",
            schema = Schema(type = "string", example = "ALBUM,SINGLE", defaultValue = "ALBUM"),
            required = true
        )
        @RequestParam(required = true) tipo: String,
    ): ResponseEntity<ResultsResponse> =
        ResponseEntity.ok(service.buscaPorArtista(nome, regiao, tipo))


    @Operation(
        summary = "Comparative search for podcast episodes",
        description = "Queries the number of available episodes for a podcast across Spotify, Deezer, and Apple Podcasts."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Search completed successfully across podcast directories",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ResultsResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters (e.g. blank name or missing required parameters)",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorMessageResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error or invalid region parameter",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorMessageResponse::class))]
            )
        ]
    )
    @GetMapping("/podcast")
    suspend fun buscaPodcast(
        @Parameter(
            name = "nome",
            description = "Podcast name to search for",
            example = "NerdCast",
            required = true
        )
        @RequestParam(required = true) nome: String,

        @Parameter(
            name = "regiao",
            description = "Country / market code for the query.\n\nAccepted values: `BR`, `US`",
            schema = Schema(type = "string", allowableValues = ["BR", "US"], defaultValue = "BR", example = "BR"),
            required = true
        )
        @RequestParam(required = true) regiao: String,
    ): ResponseEntity<ResultsResponse> =
        ResponseEntity.ok(service.buscaPorPodcast(nome, regiao))
}