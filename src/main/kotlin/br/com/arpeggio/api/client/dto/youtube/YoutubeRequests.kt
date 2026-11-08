package br.com.arpeggio.api.client.dto.youtube

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class YoutubeSearchRequest(
    val context: YoutubeRequestContext = YoutubeRequestContext(),
    val query: String,
    val params: String = "Eg-KAQwIABAAGAAgASgAMABqChAEEAMQCRAFEAo=",
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class YoutubeBrowseRequest(
    val context: YoutubeRequestContext = YoutubeRequestContext(),
    val browseId: String,
    val params: String? = null,
)

data class YoutubeRequestContext(
    val client: YoutubeClientInfo = YoutubeClientInfo(),
)

data class YoutubeClientInfo(
    val clientName: String = "WEB_REMIX",
    val clientVersion: String = "1.20240101.01.00",
    val hl: String = "pt",
    val gl: String = "BR",
)
