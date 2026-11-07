package br.com.arpeggio.api.client.dto.spotify

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpotifyApiAutheticationResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: String,
)