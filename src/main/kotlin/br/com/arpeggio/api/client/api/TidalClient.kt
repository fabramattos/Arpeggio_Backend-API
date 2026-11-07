package br.com.arpeggio.api.client.api

import br.com.arpeggio.api.infra.exception.FalhaAoBuscarArtistasException
import br.com.arpeggio.api.infra.log.Logs
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class TidalClient(
    private val authentication: TidalAuthentication,
    private val webClient: WebClient,
) {
    private val NOME_STREAMING = "Tidal"

    suspend fun atualizaToken() {
        authentication.atualizaToken(webClient)
    }

    suspend fun buscaArtistas(
        termo: String,
        regiao: String,
        idRequisicao: Int = 0,
    ): String {
        val uri = UriComponentsBuilder
            .fromUriString("https://openapi.tidal.com/v2/searchResults")
            .queryParam("countryCode", regiao)
            .queryParam("filter[query]", "{query}")
            .queryParam("include", "artists")
            .buildAndExpand(termo)
            .encode()
            .toUri()

        repeat(2) {
            try {
                Logs.info("ENTRY: TidalClient/buscaArtistas", idRequisicao)
                return webClient
                    .get()
                    .uri { uri }
                    .header("accept", "application/vnd.api+json")
                    .header("Authorization", authentication.headerValue)
                    .retrieve()
                    .awaitBody<String>()
            } catch (ex: Exception) {
                Logs.error(NOME_STREAMING, idRequisicao, ex.localizedMessage, ex)
                atualizaToken()
            }
        }
        throw FalhaAoBuscarArtistasException()
    }

    suspend fun buscaAlbunsDoArtista(
        idArtista: String,
        regiao: String,
        idRequisicao: Int = 0,
    ): String {
        val uri = UriComponentsBuilder
            .fromUriString("https://openapi.tidal.com/v2/artists/{idArtista}")
            .queryParam("countryCode", regiao)
            .queryParam("include", "albums")
            .buildAndExpand(idArtista)
            .encode()
            .toUri()

        return buscaPorUri(uri, idRequisicao)
    }

    suspend fun buscaPorUri(uri: URI, idRequisicao: Int = 0): String {
        var error: Exception? = null
        repeat(2) {
            try {
                Logs.info("ENTRY: TidalClient/buscaPorUri", idRequisicao)
                return webClient
                    .get()
                    .uri { uri }
                    .header("accept", "application/vnd.api+json")
                    .header("Authorization", authentication.headerValue)
                    .retrieve()
                    .awaitBody<String>()
            } catch (ex: Exception) {
                error = ex
                Logs.error(NOME_STREAMING, idRequisicao, ex.localizedMessage, ex)
            }
        }
        throw RuntimeException(error)
    }
}
