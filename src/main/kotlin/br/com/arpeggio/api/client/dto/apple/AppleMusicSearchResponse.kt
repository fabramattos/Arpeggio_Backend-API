package br.com.arpeggio.api.client.dto.apple

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AppleMusicSearchResponse(
    val resultCount: Int = 0,
    val results: List<AppleMusicResultItem> = emptyList(),
)
