package br.com.arpeggio.api.client.dto.spotify

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpotifyApiArtists(
    val items: List<SpotifyApiArtistData>,
)