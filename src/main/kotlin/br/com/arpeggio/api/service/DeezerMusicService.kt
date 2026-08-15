package br.com.arpeggio.api.service

import br.com.arpeggio.api.dto.externalApi.deezer.DeezerApiAlbumData
import br.com.arpeggio.api.dto.externalApi.deezer.DeezerApiAlbumsResponse
import br.com.arpeggio.api.dto.externalApi.deezer.DeezerApiArtistData
import br.com.arpeggio.api.dto.externalApi.deezer.DeezerApiArtistsResponse
import br.com.arpeggio.api.dto.request.RequestParams
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
class DeezerMusicService(
    override val NOME_STREAMING: String = "Deezer",
    val webClient: WebClient
) : CommandStreamingMusic {

    private suspend fun buscaArtista(request: RequestParams): DeezerApiArtistData {
        repeat(2) {
            try {
                Logs.info("ENTRY: DeezerMusicService/buscaPorArtista", request.id)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.deezer.com/search/artist"))
                    .queryParam("q", request.busca)
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .retrieve()
                    .awaitBody<DeezerApiArtistsResponse>()
                    .data
                    .first()

            } catch (ex: Exception) {
                Logs.warn(NOME_STREAMING, request.id, ex.localizedMessage ?: "Erro na busca de artista")
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarArtistasException()
    }

    private suspend fun buscaAlbunsDoArtista(requestParams: RequestParams, idArtista: Int): List<DeezerApiAlbumData> {
        repeat(2) {
            try {
                Logs.info("ENTRY: DeezerMusicService/buscaAlbunsDoArtista", requestParams.id)
                val uri = UriComponentsBuilder
                    .fromUri(URI("https://api.deezer.com/artist/$idArtista/albums"))
                    .queryParam("limit", 999)
                    .buildAndExpand()
                    .toUri()

                return webClient
                    .get()
                    .uri { uri }
                    .retrieve()
                    .awaitBody<DeezerApiAlbumsResponse>()
                    .data
                    .filter { album ->
                        requestParams.tipos.any { album.record_type.equals(it.name, true) }
                    }

            } catch (ex: Exception) {
                Logs.error(NOME_STREAMING, requestParams.id, ex.localizedMessage ?: "Erro na busca de albuns")
                delay(500.milliseconds)
            }
        }
        throw FalhaAoBuscarAlbunsDoArtista()
    }

    override suspend fun buscaPorArtista(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: DeezerMusicService/buscaPorArtista", requestParams.id)
        return try {
            val artista = buscaArtista(requestParams)
            val totalDeAlbuns = buscaAlbunsDoArtista(requestParams, artista.id).size
            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = artista.name,
                albuns = totalDeAlbuns
            )
        } catch (ex: Exception) {
            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = requestParams.busca,
                erro = ex.message
            )
        }
    }

}
