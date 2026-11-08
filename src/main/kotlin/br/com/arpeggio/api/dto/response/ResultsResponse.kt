package br.com.arpeggio.api.dto.response

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Consolidated response containing search results across all queried streaming services")
data class ResultsResponse(
    @field:Schema(
        description = "Original query term or artist/podcast name searched",
        example = "Metallica"
    )
    val busca: String,

    @field:ArraySchema(
        schema = Schema(description = "List of individual results for each queried streaming platform")
    )
    val resultados: List<ItemResponse>,
)