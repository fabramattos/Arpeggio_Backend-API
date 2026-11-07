package br.com.arpeggio.api.client.api

import br.com.arpeggio.api.client.dto.spotify.SpotifyApiAlbumsResponse
import br.com.arpeggio.api.client.dto.spotify.SpotifyApiArtistsResponse
import br.com.arpeggio.api.client.dto.spotify.SpotifyApiPodcastsResponse
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarAlbunsDoArtista
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarArtistasException
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarPodcastsException
import br.com.arpeggio.api.infra.log.Logs
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

@Component
class SpotifyClient(
    private val authentication: SpotifyAuthentication,
    private val webClient: WebClient,
) {
    private val NOME_STREAMING = "Spotify"

    suspend fun buscaArtistas(
        termo: String,
        regiao: String,
        limite: Int = 3,
        idRequisicao: Int = 0,
    ): SpotifyApiArtistsResponse {
        repeat(2) {
            try {
                Logs.info("ENTRY: SpotifyClient/buscaArtistas", idRequisicao)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.spotify.com/v1/search"))
                    .queryParam("q", termo)
                    .queryParam("type", "artist")
                    .queryParam("market", regiao)
                    .queryParam("limit", limite)
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .header("Authorization", authentication.headerValue)
                    .retrieve()
                    .awaitBody<SpotifyApiArtistsResponse>()
            } catch (ex: Exception) {
                if (ex.localizedMessage?.contains("401") == true) {
                    Logs.authenticationWarn(NOME_STREAMING)
                    authentication.atualizaToken()
                } else {
                    Logs.error(NOME_STREAMING, idRequisicao, ex.localizedMessage ?: "Erro na busca", ex)
                    delay(500.milliseconds)
                }
            }
        }
        throw FalhaAoBuscarArtistasException()
    }

    suspend fun buscaAlbuns(
        idArtista: String,
        includeGroups: String,
        market: String,
        limite: Int = 1,
        idRequisicao: Int = 0,
    ): SpotifyApiAlbumsResponse {
        repeat(2) {
            try {
                Logs.info("ENTRY: SpotifyClient/buscaAlbuns", idRequisicao)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.spotify.com/v1/artists/${idArtista}/albums"))
                    .queryParam("include_groups", includeGroups)
                    .queryParam("market", market)
                    .queryParam("limit", limite)
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .header("Authorization", authentication.headerValue)
                    .retrieve()
                    .awaitBody<SpotifyApiAlbumsResponse>()
            } catch (ex: Exception) {
                if (ex.localizedMessage?.contains("401") == true) {
                    Logs.authenticationWarn(NOME_STREAMING)
                    authentication.atualizaToken()
                } else {
                    Logs.error(NOME_STREAMING, idRequisicao, ex.localizedMessage ?: "Erro na busca", ex)
                    delay(500.milliseconds)
                }
            }
        }
        throw FalhaAoBuscarAlbunsDoArtista()
    }

    suspend fun buscaPodcasts(
        termo: String,
        regiao: String,
        limite: Int = 3,
        idRequisicao: Int = 0,
    ): SpotifyApiPodcastsResponse {
        repeat(2) {
            try {
                Logs.info("ENTRY: SpotifyClient/buscaPodcasts", idRequisicao)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.spotify.com/v1/search"))
                    .queryParam("q", termo)
                    .queryParam("type", "show")
                    .queryParam("market", regiao)
                    .queryParam("limit", limite)
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .header("Authorization", authentication.headerValue)
                    .retrieve()
                    .awaitBody<SpotifyApiPodcastsResponse>()
            } catch (ex: Exception) {
                if (ex.localizedMessage?.contains("401") == true) {
                    Logs.authenticationWarn(NOME_STREAMING)
                    authentication.atualizaToken()
                } else {
                    Logs.error(NOME_STREAMING, idRequisicao, ex.localizedMessage ?: "Erro na busca", ex)
                    delay(500.milliseconds)
                }
            }
        }
        throw FalhaAoBuscarPodcastsException()
    }
}
