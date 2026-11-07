package br.com.arpeggio.api.service

import br.com.arpeggio.api.client.api.DeezerClient
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.response.ItemResponse
import br.com.arpeggio.api.infra.log.Logs
import org.springframework.stereotype.Service

@Service
class DeezerMusicService(
    override val NOME_STREAMING: String = "Deezer",
    private val deezerClient: DeezerClient,
) : CommandStreamingMusic {

    override suspend fun buscaPorArtista(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: DeezerMusicService/buscaPorArtista", requestParams.id)
        return try {
            val artista = deezerClient.buscaArtistas(
                termo = requestParams.busca,
                idRequisicao = requestParams.id,
            ).data.first()

            val albunsFiltrados = deezerClient.buscaAlbuns(
                idArtista = artista.id,
                idRequisicao = requestParams.id,
            ).data.filter { album ->
                requestParams.tipos.any { album.record_type.equals(it.name, ignoreCase = true) }
            }

            ItemResponse(
                streaming = NOME_STREAMING,
                consulta = artista.name,
                albuns = albunsFiltrados.size
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
