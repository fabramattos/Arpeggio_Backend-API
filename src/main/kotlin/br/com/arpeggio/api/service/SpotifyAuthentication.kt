package br.com.arpeggio.api.service

import br.com.arpeggio.api.dto.externalApi.spotify.SpotifyApiAutheticationResponse
import br.com.arpeggio.api.infra.exception.FalhaNaAutenticacaoException
import br.com.arpeggio.api.infra.log.Logs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import kotlin.time.Duration.Companion.milliseconds

@Component
class SpotifyAuthentication(
    @Value("\${secrets.spotify_api.id}")
    private val SPOTIFY_API_ID: String,

    @Value("\${secrets.spotify_api.secret}")
    private val SPOTIFY_API_SECRET: String,
    private val webClient: WebClient,
    private var header: String? = null,
    private var token: String? = null,
) {

    val headerValue get() = header

    @Scheduled(fixedRate = 3600000L)
    fun agendarAtualizacaoToken() {
        CoroutineScope(Dispatchers.Default).launch {
            atualizaToken()
        }
    }

    suspend fun atualizaToken(attempt: Int = 1, maxAttempts: Int = 3) {
        if(attempt >= maxAttempts)
            throw FalhaNaAutenticacaoException()

        val chamadaApi = runCatching {
            token = autentica().access_token
            header = "Bearer $token"
        }

        chamadaApi.onSuccess {
            Logs.authenticated("Spotify")
            return
        }

        chamadaApi.onFailure {
            Logs.error("Spotify", 0, it.localizedMessage)
            delay(1000.milliseconds)
            atualizaToken(attempt + 1)
        }
    }

    private suspend fun autentica(): SpotifyApiAutheticationResponse {
        return webClient.post()
            .uri("https://accounts.spotify.com/api/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("grant_type=client_credentials&client_id=${SPOTIFY_API_ID}&client_secret=${SPOTIFY_API_SECRET}")
            .retrieve()
            .awaitBodyOrNull<SpotifyApiAutheticationResponse>()
            ?: throw FalhaNaAutenticacaoException()
    }
}

