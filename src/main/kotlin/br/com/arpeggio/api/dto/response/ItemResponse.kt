package br.com.arpeggio.api.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Consolidated result for a specific streaming service")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ItemResponse(
    @field:Schema(
        description = "Streaming platform name",
        example = "Spotify"
    )
    val streaming: String,

    @field:Schema(
        description = "Queried artist or podcast name",
        example = "Metallica"
    )
    val consulta: String,

    @field:Schema(
        description = "Total number of available episodes (present only in podcast queries)",
        example = "1742",
        nullable = true
    )
    val episodios: Int? = null,

    @field:Schema(
        description = "Total number of releases found matching the filter (present only in artist queries)",
        example = "22",
        nullable = true
    )
    val albuns: Int? = null,

    @field:Schema(
        description = "Error message if the query failed for this specific streaming service",
        example = "Artist not found",
        nullable = true
    )
    val erro: String? = null,
)