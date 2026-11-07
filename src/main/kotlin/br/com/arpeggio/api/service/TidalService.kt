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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private const val DELAY_REQUEST = 300L
private const val DELAY_RETRY = 1000L
private const val MAX_PAGINAS = 30
private const val VALIDADE_TOKEN = 86400 * 1000L


@Service
class TidalService(
    override val NOME_STREAMING: String = "Tidal",
    private val authentication: TidalAuthentication,
    private val webClient: WebClient,
) : CommandStreamingMusic, CommandStreamingPodcast {

    @Scheduled(fixedRate = VALIDADE_TOKEN)
    fun atualizaToken() {
        CoroutineScope(Dispatchers.Default)
            .launch {
                authentication.atualizaToken(webClient)
            }
    }

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

        return try {
            val rootNode = ObjectMapper().readTree(response)
            extrairArtistaDoJson(rootNode, requestParams.busca)
        } catch (ex: Exception) {
            if (ex is ArtistaNaoEncontradoException) throw ex
            throw FalhaAoBuscarArtistasException()
        }
    }

    private fun extrairArtistaDoJson(rootNode: JsonNode, buscaPadrao: String): TidalResult {
        val primeiroArtistaId = rootNode
            .path("data")
            .firstOrNull()
            ?.path("relationships")
            ?.path("artists")
            ?.path("data")
            ?.firstOrNull()
            ?.path("id")
            ?.asText()
            ?.takeIf { it.isNotBlank() }
            ?: throw ArtistaNaoEncontradoException()

        val artistaNome = rootNode
            .path("included")
            .firstOrNull { it.path("id").asText() == primeiroArtistaId }
            ?.path("attributes")
            ?.path("name")
            ?.asText()
            ?: buscaPadrao

        return TidalResult(primeiroArtistaId, artistaNome, 0)
    }

    private suspend fun chamadaApiTidal_BuscaArtistas(requestParams: RequestParams, uri: URI): String {
        repeat(2) {
            try {
                Logs.info("ENTRY: TidalService/chamadaApiTidal_BuscaArtistas", requestParams.id)
                return webClient
                    .get()
                    .uri { uri }
                    .header("accept", "application/vnd.api+json")
                    .header("Authorization", authentication.headerValue)
                    .retrieve()
                    .awaitBody<String>()
            } catch (ex: Exception) {
                Logs.error(NOME_STREAMING, requestParams.id, ex.localizedMessage, ex)
                authentication.atualizaToken(webClient)
            }
        }
        throw FalhaAoBuscarArtistasException()
    }


    private fun uriBuscaArtistas(requestParams: RequestParams): URI =
        UriComponentsBuilder
            .fromUriString("https://openapi.tidal.com/v2/searchResults")
            .queryParam("countryCode", requestParams.regiao.name)
            .queryParam("filter[query]", "{query}")
            .queryParam("include", "artists")
            .buildAndExpand(requestParams.busca)
            .encode()
            .toUri()


    private suspend fun buscaAlbunsDoArtista(requestParams: RequestParams, idArtista: String): Int {
        var errosReq = 0
        val lancamentosAcumulados = mutableListOf<LancamentoTidal>()
        var paginaAtual = 0
        var proximaUri: URI? = uriAlbunsDoArtista(requestParams, idArtista)

        while (proximaUri != null && paginaAtual < MAX_PAGINAS && errosReq <= 3) {
            val uriAtual = proximaUri
            val resultado = runCatching {
                val response = chamadaApiTidal_AlbunsDoArtista(requestParams, uriAtual)
                val rootNode = ObjectMapper().readTree(response)

                val lancamentosDaPagina = extrairLancamentosDaPagina(rootNode, requestParams)
                lancamentosAcumulados.addAll(lancamentosDaPagina)

                extrairProximaUri(rootNode)
            }

            resultado.onSuccess { nextUri ->
                proximaUri = nextUri
                paginaAtual++
                delay(DELAY_REQUEST)
            }

            resultado.onFailure {
                if (it is FalhaInformacoesImprecisasDireitosAutoraisException) throw it
                val is429 = it.localizedMessage?.contains("429") == true || it.cause?.localizedMessage?.contains("429") == true
                if (is429) {
                    errosReq++
                    delay(DELAY_RETRY)
                } else {
                    throw FalhaAoBuscarAlbunsDoArtista()
                }
            }
        }

        val lancamentosSemDuplicatas = removerDuplicatas(lancamentosAcumulados)
        return lancamentosSemDuplicatas.size
    }

    private fun removerDuplicatas(lancamentos: List<LancamentoTidal>): List<LancamentoTidal> {
        return lancamentos.distinctBy { lancamento ->
            gerarChaveDeduplicacao(lancamento)
        }
    }

    private fun gerarChaveDeduplicacao(lancamento: LancamentoTidal): String {
        if (lancamento.titulo.isBlank()) return lancamento.id
        val tituloNormalizado = lancamento.titulo
            .replace("’", "'")
            .replace("…", "...")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
        return "${lancamento.tipo.uppercase()}_$tituloNormalizado"
    }

    private fun extrairLancamentosDaPagina(
        rootNode: JsonNode,
        requestParams: RequestParams,
    ): List<LancamentoTidal> {
        val albunsNode = rootNode.path("included")
        return albunsNode.mapNotNull { albumNode ->
            validarRestricaoAutoral(albumNode, requestParams)

            val tipo = extrairTipoLancamento(albumNode)
            val titulo = albumNode.path("attributes").path("title").asText()
            val id = albumNode.path("id").asText()

            if (ehTipoPermitido(tipo, requestParams)) {
                LancamentoTidal(id, tipo, titulo)
            } else {
                null
            }
        }
    }

    private fun validarRestricaoAutoral(albumNode: JsonNode, requestParams: RequestParams) {
        val status = albumNode.path("status").asInt()
        if (status == 451 && !verificaSePodeIgnorarRestricaoAutoral(requestParams)) {
            throw FalhaInformacoesImprecisasDireitosAutoraisException()
        }
    }

    private fun extrairTipoLancamento(albumNode: JsonNode): String {
        return albumNode.path("attributes").path("type").asText()
            .ifBlank { albumNode.path("attributes").path("albumType").asText() }
            .ifBlank { albumNode.path("resource").path("type").asText() }
    }

    private fun ehTipoPermitido(tipo: String, requestParams: RequestParams): Boolean {
        return if (tipo.isBlank()) {
            verificaSePodeIgnorarRestricaoAutoral(requestParams)
        } else {
            requestParams.tipos.any { it.name.equals(tipo, ignoreCase = true) }
        }
    }

    private fun extrairProximaUri(rootNode: JsonNode): URI? {
        val rawNext = rootNode.path("links").path("next").asText().takeIf { it.isNotBlank() }
            ?: rootNode.path("data").path("relationships").path("albums").path("links").path("next").asText().takeIf { it.isNotBlank() }
            ?: return null

        val fullUrl = if (rawNext.startsWith("http")) rawNext else "https://openapi.tidal.com/v2$rawNext"
        val urlComInclude = if (!fullUrl.contains("include=albums")) {
            if (fullUrl.contains("?")) "$fullUrl&include=albums" else "$fullUrl?include=albums"
        } else {
            fullUrl
        }
        return URI(urlComInclude)
    }

    private suspend fun chamadaApiTidal_AlbunsDoArtista(request: RequestParams, uri: URI): String {
        var error: Exception? = null
        repeat(2) {
            try {
                Logs.info("ENTRY: TidalService/chamadaApiTidal_AlbunsDoArtista", request.id)
                return webClient
                    .get()
                    .uri { uri }
                    .header("accept", "application/vnd.api+json")
                    .header("Authorization", authentication.headerValue)
                    .retrieve()
                    .awaitBody<String>()
            } catch (ex: Exception) {
                error = ex
                Logs.error(NOME_STREAMING, request.id, ex.localizedMessage, ex)
            }
        }
        throw RuntimeException(error)
    }

    private fun uriAlbunsDoArtista(requestParams: RequestParams, idArtista: String): URI =
        UriComponentsBuilder
            .fromUriString("https://openapi.tidal.com/v2/artists/{idArtista}")
            .queryParam("countryCode", requestParams.regiao.name)
            .queryParam("include", "albums")
            .buildAndExpand(idArtista)
            .encode()
            .toUri()

    private fun verificaSePodeIgnorarRestricaoAutoral(requestParams: RequestParams): Boolean {
        return requestParams
            .tipos.containsAll(listOf(RequestTipo.ALBUM, RequestTipo.SINGLE))
    }
}

private data class LancamentoTidal(
    val id: String,
    val tipo: String,
    val titulo: String,
)