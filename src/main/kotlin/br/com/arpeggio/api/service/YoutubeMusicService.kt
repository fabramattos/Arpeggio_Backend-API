package br.com.arpeggio.api.service

import br.com.arpeggio.api.client.api.YoutubeClient
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.response.ItemResponse
import org.springframework.stereotype.Service

@Service
class YoutubeMusicService(
    override val NOME_STREAMING: String = "Youtube Music",
    private val youtubeClient: YoutubeClient,
) : CommandStreamingMusic, CommandStreamingPodcast {

    override suspend fun buscaPorArtista(requestParams: RequestParams): ItemResponse {
        /*
        Logs.info("ENTRY: Youtube/buscaPorArtista | Request = $requestParams")
        val busca = runCatching { youtubeClient.executaSelenium(requestParams) }
        val artista = busca.getOrNull()

        return if (artista == null)
            ResultadoBuscaErros(NOME_STREAMING, busca.exceptionOrNull()!!.localizedMessage)
        else
            ResultadoBuscaConcluidaAlbuns(NOME_STREAMING, artista.name, artista.qty)
        */

        return ItemResponse(
            streaming = NOME_STREAMING,
            consulta = requestParams.busca,
            erro = "Serviço temporariamente desabilitado"
        )
    }

    override suspend fun buscaPorPodcast(requestParams: RequestParams): ItemResponse {
        return ItemResponse(
            streaming = NOME_STREAMING,
            consulta = requestParams.busca,
            erro = "busca por podcast ainda não implementada"
        )
    }
}