package br.com.arpeggio.api.client.api

import br.com.arpeggio.api.infra.exception.FalhaNaAutenticacaoException
import br.com.arpeggio.api.infra.log.Logs
import br.com.arpeggio.api.infra.security.AuthEncoders
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.delay
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import kotlin.time.Duration.Companion.milliseconds

@Component
class TidalAuthentication(
    @Value("\${secrets.tidal_api.id}")
    private val TIDAL_API_ID: String,

    @Value("\${secrets.tidal_api.secret}")
    private val TIDAL_API_SECRET: String,
    private val B64CREDENTIALS: String = AuthEncoders().encodeToBase64(TIDAL_API_ID, TIDAL_API_SECRET),
    private var token: String? = null,
    private var header: String? = null,
) {
    val headerValue get() = header

    suspend fun atualizaToken(webClient: WebClient, attempt: Int = 1, maxAttempts: Int = 3) {
        if (attempt >= maxAttempts)
            throw FalhaNaAutenticacaoException()

        val chamadaApi = runCatching {
            token = autentica(webClient)
            header = "Bearer $token"
        }

        chamadaApi.onSuccess {
            Logs.authenticated("Tidal")
            return
        }

        chamadaApi.onFailure {
            Logs.error("Tidal", 0, it.localizedMessage)
            delay(500.milliseconds)
            atualizaToken(webClient, attempt + 1)
        }
    }

    private suspend fun autentica(webClient: WebClient): String {
        val json = webClient.post()
            .uri("https://auth.tidal.com/v1/oauth2/token")
            .header("Authorization", B64CREDENTIALS)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("grant_type", "client_credentials"))
            .retrieve()
            .awaitBodyOrNull<String>()
            ?: throw FalhaNaAutenticacaoException()

        return ObjectMapper()
            .readTree(json)
            .path("access_token")
            .asText()
    }
}
