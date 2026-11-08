package br.com.arpeggio.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Standard error response returned by Arpeggio API on invalid requests or validation errors")
data class ApiErrorMessageResponse(
    @field:Schema(
        description = "Descriptive error message explaining the request failure",
        example = "Parameter nome not provided!"
    )
    val erroApi: String,
)