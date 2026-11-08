package br.com.arpeggio.api.client.dto.youtube

data class YoutubeItemDto(
    val id: String,
    val title: String,
    val type: String,
    val year: String? = null,
)
