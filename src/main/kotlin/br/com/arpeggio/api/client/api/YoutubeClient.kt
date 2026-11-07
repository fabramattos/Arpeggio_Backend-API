package br.com.arpeggio.api.client.api

import br.com.arpeggio.api.client.dto.youtube.YoutubeResult
import br.com.arpeggio.api.dto.request.RequestParams
import br.com.arpeggio.api.dto.request.RequestTipo
import br.com.arpeggio.api.infra.log.Logs
import jakarta.annotation.PostConstruct
import org.openqa.selenium.By
import org.openqa.selenium.By.cssSelector
import org.openqa.selenium.By.xpath
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.PageLoadStrategy
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Wait
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration

private const val XPATH_BOTAO_ARTISTA: String = "//yt-formatted-string[text()='Artistas']"
private const val XPATH_CABECALHO_LISTA_ARTISTAS: String =
    "//*[@id=\"contents\"]/ytmusic-shelf-renderer/div[1]/h2/yt-formatted-string"
private const val CSS_LISTA_ARTISTAS_PRIMEIRO_ITEM: String =
    "#contents > ytmusic-responsive-list-item-renderer:nth-child(1) > a"

private const val CSS_BOTAO_ALBUNS_DO_ARTISTA_AGUARDAR: String = "#details > yt-formatted-string"
private const val CSS_BOTAO_ALBUNS_DO_ARTISTA_LINK: String = "#details > yt-formatted-string > a"
private const val CSS_LISTA_ALBUNS_ARTISTA: String = "#contents > ytmusic-grid-renderer"

@Component
class YoutubeClient(
    @Value("\${webdriver.host}")
    private val WEBDRIVER_HOST: String,

    @Value("\${webdriver.port}")
    private val WEBDRIVER_PORT: String,

    private var driver: WebDriver? = null,
) {

    @PostConstruct
    fun logaUrlDoChrome() {
        Logs.info("Selenium: url do driver: ${WEBDRIVER_HOST}:${WEBDRIVER_PORT}", 0)
    }

    fun executaSelenium(requestParams: RequestParams): YoutubeResult {
        val options = EdgeOptions()
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL)
        options.addArguments("--ignore-ssl-errors=yes")
        options.addArguments("--ignore-certificate-errors")
        options.addArguments("--lang=pt-BR")

        val navegacao = runCatching {
            driver = RemoteWebDriver(URI("${WEBDRIVER_HOST}:${WEBDRIVER_PORT}").toURL(), options)

            driver?.let {
                val wait: Wait<WebDriver> = WebDriverWait(it, Duration.ofSeconds(10))
                val url = "https://music.youtube.com/search?q=${requestParams.busca}"

                it.get(url)
                cliqueBotaoArtista(wait)
                val artista = cliqueNoArtistaDaLista(wait)
                cliqueBotaoAlbumDoArtista(wait)
                artista.apply { qty = retornaTodosAlbuns(wait, requestParams) }
                return@runCatching artista
            } ?: throw RuntimeException("Falha ao criar Remote Web Driver")
        }

        driver?.quit()

        return navegacao.getOrElse {
            throw RuntimeException(navegacao.exceptionOrNull()?.localizedMessage ?: "Erro na execução do Selenium")
        }
    }

    private fun retornaTodosAlbuns(wait: Wait<WebDriver>, requestParams: RequestParams): Int {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(CSS_LISTA_ALBUNS_ARTISTA)))

        val listaItens = javaScript_retornaTodosItensDoArtista()

        val listaItensEnum: List<RequestTipo> = listaItens.map {
            when (it) {
                "ALBUM" -> RequestTipo.ALBUM
                "SINGLE" -> RequestTipo.SINGLE
                else -> RequestTipo.EP
            }
        }

        return listaItensEnum
            .filter { requestParams.tipos.contains(it) }
            .size
    }

    private fun javaScript_retornaTodosItensDoArtista(): MutableList<String> {
        val script = """
                var itemsContainer = document.querySelector('#contents > ytmusic-grid-renderer > #items');
    
                if (itemsContainer) {
                    var items = itemsContainer.children;
                    var listaItens = [];

                    for (var i = 0; i < items.length; i++) {
                        var item = items[i];
                        var details = item.querySelector('div.details.style-scope.ytmusic-two-row-item-renderer');
                        var texto = details.querySelector('span > yt-formatted-string > span:nth-child(1)').innerText;
                        
                        switch(texto) {
                            case 'Álbum':
                                listaItens.push('ALBUM');
                                break;
                            case 'Single':
                                listaItens.push('SINGLE');
                                break;
                            case 'EP':
                                listaItens.push('EP');
                                break;
                        }
                    }

                    return listaItens;
                } else {
                    return null;
                }
            """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        return (driver as JavascriptExecutor).executeScript(script) as MutableList<String>
    }

    private fun cliqueBotaoAlbumDoArtista(wait: Wait<WebDriver>) {
        wait.until(ExpectedConditions.presenceOfElementLocated(cssSelector(CSS_BOTAO_ALBUNS_DO_ARTISTA_AGUARDAR)))

        val linkAlbuns = driver?.findElement(cssSelector(CSS_BOTAO_ALBUNS_DO_ARTISTA_LINK))
            ?: throw RuntimeException("Erro ao recuperar botão ALBUNS")

        driver?.get(linkAlbuns.getAttribute("href"))
            ?: throw RuntimeException("Erro ao tentar acessar ALBUNS do artista")
    }

    private fun cliqueNoArtistaDaLista(wait: Wait<WebDriver>): YoutubeResult {
        wait.until(
            ExpectedConditions.textToBePresentInElementLocated(
                xpath(XPATH_CABECALHO_LISTA_ARTISTAS),
                "Artistas"
            )
        )

        val itemArtista = driver?.findElement(cssSelector(CSS_LISTA_ARTISTAS_PRIMEIRO_ITEM))
            ?: throw RuntimeException("Erro ao recuperar artista")

        val artista = YoutubeResult(
            itemArtista.getAttribute("href"),
            itemArtista.getAttribute("aria-label"),
            0
        )

        itemArtista.click()
        return artista
    }

    private fun cliqueBotaoArtista(wait: Wait<WebDriver>) {
        wait.until(ExpectedConditions.presenceOfElementLocated(xpath(XPATH_BOTAO_ARTISTA)))
            .click()
    }
}
