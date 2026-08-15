package br.com.arpeggio.api.service

import br.com.arpeggio.api.dto.externalApi.tidal.TidalResult
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.request.RequestTipo
import br.com.arpeggio.api.dto.response.ItemResponse
import br.com.arpeggio.api.infra.exception.ArtistaNaoEncontradoException
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarAlbunsDoArtista
import br.com.arpeggio.api.infra.exception.FalhaAoBuscarArtistasException
import br.com.arpeggio.api.infra.exception.FalhaInformacoesImprecisasDireitosAutoraisException
import br.com.arpeggio.api.infra.log.Logs
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private const val DELAY_REQUEST = 800L
private const val DELAY_RETRY = 1000L
private const val LIMIT = 90
private const val VALIDADE_TOKEN = 86400 * 1000L


@Service
class TidalService(
    override val NOME_STREAMING: String = "Tidal",
    private val authentication: TidalAuthentication,
    private val webClient: WebClient,
) : CommandStreamingMusic, CommandStreamingPodcast {

//    @Scheduled(fixedRate = VALIDADE_TOKEN)
//    fun atualizaToken() {
//        CoroutineScope(Dispatchers.Default)
//            .launch {
//                authentication.atualizaToken(webClient)
//            }
//    }

    override suspend fun buscaPorArtista(requestParams: RequestParams): ItemResponse {
        Logs.info("ENTRY: TidalService/buscaPorArtista", requestParams.id)
        try {
            val artista = buscaArtista(requestParams)
                .apply { qty = buscaAlbunsDoArtista(requestParams, id) }

            return ItemResponse(
                streaming = NOME_STREAMING,
                consulta = artista.name,
                albuns = artista.qty
            )
        } catch (ex: Exception) {
            return ItemResponse(
                streaming = NOME_STREAMING,
                consulta = requestParams.busca,
                erro = ex.message
            )
        }
    }

    override suspend fun buscaPorPodcast(requestParams: RequestParams): ItemResponse {
        return ItemResponse(
            streaming = NOME_STREAMING,
            consulta = requestParams.busca,
            erro = "busca por podcast ainda não implementada"
        )
    }

    private suspend fun buscaArtista(requestParams: RequestParams): TidalResult {
        val uri = uriBuscaArtistas(requestParams)
        val response = chamadaApiTidal_BuscaArtistas(requestParams, uri)

        try {
            val artistaDados = ObjectMapper()
                .readTree(response)
                .path("artists") // array de artistas. Dentro, RESOURCES contem os dados de cada artista retornado
                .first()
                .path("resource")
                ?: throw ArtistaNaoEncontradoException()

            return TidalResult(
                artistaDados.path("id").asText(),
                artistaDados.path("name").asText(),
                0
            )
        }catch (ex: Exception){
            throw FalhaAoBuscarAlbunsDoArtista()
        }
    }

    private suspend fun chamadaApiTidal_BuscaArtistas(requestParams: RequestParams, uri: URI): String {
        repeat(2) {
            try {
                Logs.info("ENTRY: TidalService/chamadaApiTidal_BuscaArtistas", requestParams.id)
                return webClient
                    .get()
                    .uri { uri }
                    .header("accept", "application/vnd.tidal.v1+json")
                    .header("Authorization", authentication.headerValue)
                    .header("Content-Type", "application/vnd.tidal.v1+json")
                    .retrieve()
                    .awaitBody<String>()
            } catch (ex: Exception) {
                Logs.error(NOME_STREAMING, requestParams.id, ex.localizedMessage)
                authentication.atualizaToken(webClient)
            }
        }
        throw FalhaAoBuscarArtistasException()
    }


    private fun uriBuscaArtistas(requestParams: RequestParams) =
        try{
            UriComponentsBuilder
                .fromUri(URI("https://openapi.tidal.com/v2/searchresults/${requestParams.busca}"))
                .queryParam("type", "ARTISTS")
                .queryParam("offset", 0)
                .queryParam("limit", 3)
                .queryParam("countryCode", requestParams.regiao.name)
                .buildAndExpand()
                .toUri()
        } catch (ex: Exception){
            throw RuntimeException("Erro na url da api Tidal")
        }


    private suspend fun buscaAlbunsDoArtista(requestParams: RequestParams, idArtista: String): Int {
        var errosReq = 0
        var offset = 0
        var totalAlbuns = 0
        var maxAlbuns = Int.MAX_VALUE
        while (offset < maxAlbuns && errosReq <= 3) {
            val resultado = runCatching {

                val uri = uriAlbunsDoArtista(requestParams, idArtista, offset)
                val response = chamadaApiTidal_AlbunsDoArtista(requestParams, uri)

                val albunsNode = ObjectMapper()
                    .readTree(response)
                    .path("data")

                maxAlbuns = ObjectMapper()
                    .readTree(response)
                    .path("metadata")
                    .path("total")
                    .asInt()

                totalAlbuns += contarAlbunsValidos(requestParams, albunsNode)
            }

            resultado.onSuccess {
                offset += LIMIT
                Thread.sleep(DELAY_REQUEST)
            }

            resultado.onFailure {
                if (it.localizedMessage.contains("Erro: 429")) { // erro de muitas requisições ao Tidal
                    errosReq++
                    Thread.sleep(DELAY_RETRY)
                } else
                    throw FalhaAoBuscarAlbunsDoArtista()
            }
        }
        return totalAlbuns
    }

    private suspend fun chamadaApiTidal_AlbunsDoArtista(request: RequestParams, uri: URI): String {
        var error: Exception? = null
        repeat(2) {
            try {
                Logs.info("ENTRY: TidalService/chamadaApiTidal_AlbunsDoArtista", request.id)
                return webClient
                    .get()
                    .uri { uri }
                    .header("accept", "application/vnd.tidal.v1+json")
                    .header("Authorization", authentication.headerValue)
                    .header("Content-Type", "application/vnd.tidal.v1+json")
                    .retrieve()
                    .awaitBody<String>()
            } catch (ex: Exception) {
                error = ex
                Logs.error(NOME_STREAMING, request.id, ex.localizedMessage)
            }
        }
        throw RuntimeException(error)
    }


    private fun uriAlbunsDoArtista(requestParams: RequestParams, idArtista: String, offset: Int) =
        UriComponentsBuilder
            .fromUri(URI("https://openapi.tidal.com/artists/${idArtista}/albums"))
            .queryParam("countryCode", requestParams.regiao.name)
            .queryParam("offset", offset)
            .queryParam("limit", LIMIT)
            .buildAndExpand()
            .toUri()


    private fun contarAlbunsValidos(requestParams: RequestParams, albunsNode: JsonNode): Int {
        return albunsNode.count { album ->
            if (verificaSePodeIgnorarRestricaoAutoral(requestParams)) {
                true
            } else {
                val type = album.path("resource").path("type").asText()
                val status = album.path("status").asInt()

                if (status == 451) throw FalhaInformacoesImprecisasDireitosAutoraisException()

                requestParams.tipos.any { tipo -> type.equals(tipo.name, true) }
            }
        }
    }

    private fun verificaSePodeIgnorarRestricaoAutoral(requestParams: RequestParams): Boolean {
        return requestParams
            .tipos.containsAll(listOf(RequestTipo.ALBUM, RequestTipo.SINGLE))
    }
}