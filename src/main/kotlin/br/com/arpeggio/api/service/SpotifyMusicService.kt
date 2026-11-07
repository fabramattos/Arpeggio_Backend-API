package br.com.arpeggio.api.service

import br.com.arpeggio.api.client.api.SpotifyClient
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.request.RequestTipo
import br.com.arpeggio.api.dto.response.ItemResponse
import br.com.arpeggio.api.infra.log.Logs
import org.springframework.stereotype.Service

@Service
class SpotifyMusicService(
    override val NOME_STREAMING: String = "Spotify",
    private val spotifyClient: SpotifyClient,
) : CommandStreamingMusic {

    override suspend fun buscaPorArtista(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: SpotifyMusicService/buscaPorArtista", requestParams.id)
        return try {
            val artista = spotifyClient.buscaArtistas(
                termo = requestParams.busca,
                regiao = requestParams.regiao.name,
                idRequisicao = requestParams.id,
            ).artists.items.first()

            val totalDeAlbuns = spotifyClient.buscaAlbuns(
                idArtista = artista.id,
                includeGroups = formataTiposParaSpotify(requestParams),
                market = requestParams.regiao.valor,
                idRequisicao = requestParams.id,
            ).total

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

    private fun formataTiposParaSpotify(requestParams: RequestParams): String {
        return requestParams.tipos
            .filterNot { it == RequestTipo.EP } // Spotify não filtra EP isolado (Single = Single + EP)
            .joinToString(",") { it.name.lowercase() }
    }
}
