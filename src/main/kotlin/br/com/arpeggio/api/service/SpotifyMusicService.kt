package br.com.arpeggio.api.service

import br.com.arpeggio.api.dto.externalApi.spotify.*
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.request.RequestTipo
import br.com.arpeggio.api.dto.response.ItemResponse
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarAlbunsDoArtista
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarArtistasException
import br.com.arpeggio.api.infra.log.Logs
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

@Service
class SpotifyMusicService(
    override val NOME_STREAMING: String = "Spotify",
    private val authentication: SpotifyAuthentication,
    private val webClient: WebClient,
) : CommandStreamingMusic {

    suspend fun buscaArtista(requestParams: RequestParams): SpotifyApiArtistData {
        repeat(2) {
            try {
                Logs.info("ENTRY: SpotifyMusicService/buscaArtista", requestParams.id)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.spotify.com/v1/search"))
                    .queryParam("q", requestParams.busca)
                    .queryParam("type", "artist")
                    .queryParam("market", requestParams.regiao.name)
                    .queryParam("limit", 3)
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .header("Authorization", authentication.headerValue)
                    .retrieve()
                    .awaitBody<SpotifyApiArtistsResponse>()
                    .artists.items
                    .first()

            } catch (ex: Exception) {
                if (ex.localizedMessage?.contains("401") == true) {
                    Logs.authenticationWarn(NOME_STREAMING)
                    authentication.atualizaToken()
                } else {
                    Logs.error(NOME_STREAMING, requestParams.id, ex.localizedMessage ?: "Erro de autenticação")
                    delay(500.milliseconds)
                }
            }
        }
        throw FalhaAoBuscarArtistasException()
    }


    suspend fun buscaAlbunsDoArtista(requestParams: RequestParams, idArtista: String): SpotifyApiAlbumsResponse {
        repeat(2) {
            try {
                Logs.info("ENTRY: SpotifyMusicService/buscaAlbunsDoArtista", requestParams.id)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.spotify.com/v1/artists/${idArtista}/albums"))
                    .queryParam("include_groups", retornaTipos(requestParams))
                    .queryParam("market", requestParams.regiao.valor)
                    .queryParam("limit", 1)
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .header("Authorization", authentication.headerValue)
                    .retrieve()
                    .awaitBody<SpotifyApiAlbumsResponse>()

            } catch (ex: Exception) {
                Logs.error(NOME_STREAMING, requestParams.id, ex.localizedMessage ?: "Erro na busca")
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarAlbunsDoArtista()
    }

    override suspend fun buscaPorArtista(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: SpotifyMusicService/buscaPorArtista", requestParams.id)
        try {
            val artista = buscaArtista(requestParams)
            val totalDeAlbuns = buscaAlbunsDoArtista(requestParams, artista.id).total
            return ItemResponse(
                streaming = NOME_STREAMING,
                consulta = artista.name,
                albuns = totalDeAlbuns
            )
        } catch (ex: Exception) {
            return ItemResponse(
                streaming = NOME_STREAMING,
                consulta = requestParams.busca,
                erro = ex.message
            )
        }
    }

    private fun retornaTipos(requestParams: RequestParams): String {
        var texto = ""

        requestParams.tipos
            .filterNot { it == RequestTipo.EP } // -> Spotify não filtra EP! Single = Single + EP
            .forEach { texto = texto.plus(it.name + ",") }

        texto = texto.removeSuffix(",")
        return texto.lowercase()
    }
}
