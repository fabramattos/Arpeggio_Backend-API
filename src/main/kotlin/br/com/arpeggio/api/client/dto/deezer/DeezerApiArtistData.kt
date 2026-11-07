package br.com.arpeggio.api.client.dto.deezer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeezerApiArtistData(
        val id: Int,
        val name: String,
        val link: String?
)