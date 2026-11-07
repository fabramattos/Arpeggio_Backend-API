package br.com.arpeggio.api.client.dto.tidal

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TidalResult(
    val id: String,
    val name: String,
    var qty: Int,
)
