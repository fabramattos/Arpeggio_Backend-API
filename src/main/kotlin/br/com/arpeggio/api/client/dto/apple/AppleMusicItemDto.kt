package br.com.arpeggio.api.client.dto.apple

data class AppleMusicItemDto(
    val id: String,
    val title: String,
    val type: String, // "ALBUM", "SINGLE", "EP"
    val year: String? = null,
)
