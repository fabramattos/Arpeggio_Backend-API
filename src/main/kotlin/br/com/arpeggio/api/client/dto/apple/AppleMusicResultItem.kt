package br.com.arpeggio.api.client.dto.apple

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AppleMusicResultItem(
    val wrapperType: String? = null,
    val artistType: String? = null,
    val artistName: String? = null,
    val artistId: Long? = null,
    val collectionId: Long? = null,
    val collectionName: String? = null,
    val collectionType: String? = null,
    val trackCount: Int? = null,
    val releaseDate: String? = null,
)
