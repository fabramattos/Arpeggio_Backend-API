package br.com.arpeggio.api.client.dto.deezer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeezerApiAlbumsResponse(
    val data: List<DeezerApiAlbumData>,
    val total: Int,
)