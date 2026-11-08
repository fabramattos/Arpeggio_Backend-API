*Change language:* [![en](https://img.shields.io/badge/lang-en-red.svg)](https://github.com/fabramattos/Arpeggio)<br>
# 🎧 Arpeggio 🎧
**Arpeggio** é um aplicativo de comparação de streamings de áudio.<br>
Com ele, o usuário pode:<br>
- ☑️ consultar o número de lançamentos (Álbuns, Singles e EPs) de um artista ou banda, presentes nos streamings.<br>
- ☑️ consultar o número de episódios de um Podcast, presentes nos streamings.<br>
- ❎ terminar uma partida de Uno sem brigar com demais jogadores.<br>

No momento, conectamos com:
- **Música:** Spotify, Deezer, Tidal, YouTube Music e Apple Music.
- **Podcasts:** Spotify, Deezer e Apple Podcasts.

🚨 **_Este repositório é relativo ao Backend utilizado pelas versões web e android_** 🚨

## 🚧 Status do projeto 🚧
![Badge em Desenvolvimento](http://img.shields.io/static/v1?label=STATUS&message=EM%20DESENVOLVIMENTO&color=GREEN&style=for-the-badge)

## 🚀 Stack 🚀
 - Java 21 (LTS)
 - Kotlin 2.1
 - Kotlin Coroutines 1.10
 - Spring Boot 3.3 (Web, WebFlux / WebClient)
 - Docker & Docker-Compose
 - OpenAPI / Swagger (SpringDoc 2.6)

## 📖 Como utilizar (API isolada sem frontend)
É possível utilizar e testar as funcionalidades da API diretamente via requisições HTTP ou pela documentação interativa do Swagger UI:

- **Host (Railway):** `https://arpeggio.up.railway.app`
- **Swagger-UI Interativo:** [**https://arpeggio.up.railway.app/swagger-ui/index.html**](https://arpeggio.up.railway.app/swagger-ui/index.html)
- **Contrato OpenAPI 3.0 (JSON para Frontends / Agentes de IA):** [**https://arpeggio.up.railway.app/swagger.json**](https://arpeggio.up.railway.app/swagger.json)
- **Status da API (Healthcheck):** `https://arpeggio.up.railway.app/health`

## 💡 Futuras implementações 💡
### Novos Streamings:
- Amazon Music:
  - Em estudo para consumo direto de endpoints REST sem dependência de automação de navegador.

### Novas funcionalidades
- [ ] cadastro de usuários

## 💻 Desenvolvedores 💻

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/fabramattos">
        <img src="https://avatars.githubusercontent.com/u/45768087?v=4" width="100" height="100">
      </a>
      <br>Felipe Mattos (Backend)</br>
    </td>
    <td align="center">
      <a href="https://github.com/FabioLiriodev">
        <img src="https://avatars.githubusercontent.com/u/140852220?s=400&u=c03075cdb745198fe290f16fd7a345907cae4c89&v=4" width="100" height="100">
      </a>
      <br>Fábio Lirio (Frontend)</br>
    </td>
  </tr>
</table>
