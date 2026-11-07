package br.com.arpeggio.api.client.dto.deezer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeezerApiAlbumData(
    val record_type: String?,
    val type: String?,
)