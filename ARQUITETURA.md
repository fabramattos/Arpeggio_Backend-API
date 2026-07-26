# Arquitetura, Docker e Fluxo de CI/CD - Arpeggio Backend

Este documento descreve a arquitetura de execução, conteinerização, gerenciamento de ambientes/segredos e o fluxo de integração e entrega contínua (CI/CD) do projeto **Arpeggio Backend**.

---

## 1. Visão Geral dos Perfis de Ambiente (Profiles)

O Spring Boot no projeto é estruturado para segregar claramente o desenvolvimento local do ambiente de produção:

* **`application.yml`**: Arquivo principal de configuração. Por padrão, aponta para o perfil ativo `prod` (`spring.profiles.active: prod`), garantindo que o servidor de deploy produtivo assuma a configuração correta sem depender de configurações extras de infraestrutura.
* **`application-dev.yml`**: Contém configurações exclusivas para o desenvolvimento local e depuração (ex.: níveis de log detalhados, mock de serviços). É ativado localmente ou pelo Docker Compose.
* **`application-prod.yml`**: Contém configurações específicas para produção, como a escuta de porta dinâmica baseada na variável de ambiente de nuvem `${PORT}` e timeouts de conexão.

---

## 2. Gerenciamento de Segredos e Variáveis (`.env`)

As credenciais sensíveis de APIs externas (Amazon, Spotify, Tidal) **nunca** devem ser hardcoded em arquivos YAML ou no Docker Compose.

* **Arquivo `.env`**: Localizado na raiz do projeto, armazena as chaves de API exclusivamente no ambiente local (ex.: `SPOTIFY_API_CLIENT_ID`, `TIDAL_API_CLIENT_SECRET`).
* **Segurança**: O arquivo `.env` está inserido no `.gitignore` e não é enviado ao repositório Git.
* **Integração com Docker**: O arquivo `.env` é lido e consumido automaticamente pelos containers locais através da diretiva `env_file: - .env` no Docker Compose.

---

## 3. Função do `Dockerfile`

O `Dockerfile` na raiz tem como papel empacotar a aplicação em um container leve e padronizado:
* Baseado em imagem oficial do Java/OpenJDK (`openjdk:21-ea-1-jdk-oracle`).
* **Dependência do Build**: Ele copia o artefato `.jar` gerado pelo Gradle (`COPY build/libs/*.jar /app/app.jar`). Portanto, antes de construir a imagem Docker, o `.jar` deve ser gerado pelo comando `./gradlew bootJar` ou `./gradlew build`.
* Expõe a porta `8080` e define o ponto de entrada executando a JVM com suporte a containers (`-XX:+UseContainerSupport`).

---

## 4. Função do `Docker-Compose.yml` (Desenvolvimento Local)

O arquivo `Docker-Compose.yml` é projetado **exclusivamente para a execução no ambiente de desenvolvimento local**, permitindo subir a API e suas dependências de forma integrada:
* **Orquestração de Serviços**: Sobe conjuntamente o container da aplicação (`arpeggio`) e o container de automação de browser (`selenium`/standalone-edge) na mesma rede (`selenium-network`).
* **Forçamento do Perfil Dev**: Para isolar o ambiente local de produção e evitar erros com variáveis exclusivas de prod (como `${PORT}`), o serviço da API define no bloco `environment`:
  ```yaml
  SPRING_PROFILES_ACTIVE: dev
  ```
  Isso sobrescreve o padrão do `application.yml`, garantindo que o container local rode sempre sob o perfil `-dev`.
* **Depuração Remota**: Expõe a porta `5005` e injeta `JAVA_TOOL_OPTIONS` com agentes JDWP para permitir o anexo de debuggers da IDE diretamente no código rodando dentro do container.
* **Consumo de Segredos**: Injeta as variáveis do `.env` local no container através de `env_file`.

---

## 5. Função da Pipeline CI/CD (GitHub Actions)

Toda a responsabilidade de compilação oficial, testes e publicação de imagens Docker é delegada às pipelines automatizadas na pasta `.github/workflows/`:

* **`PR_Master.yml`**: Executada na abertura ou atualização de Pull Requests contra a `master`. Realiza o checkout do código, configura o JDK 20 e roda a suíte de testes (`./gradlew test`) para garantir a integridade do código antes do merge.
* **`MERGED_Master.yml` e `PUSH_Master.yml`**: Acionadas ao ocorrer um merge de PR na `master` ou um push direto. Realizam o fluxo de entrega contínua:
  1. Executam o build limpo do projeto via Gradle (`./gradlew build -x test` ou `./gradlew build`).
  2. Autenticam no Docker Hub utilizando os segredos do repositório (`DOCKERHUB_USERNAME` e `DOCKERHUB_TOKEN`).
  3. Constroem a nova imagem utilizando o `Dockerfile` da raiz e executam o **push oficial** da imagem para o Docker Hub com a tag mais recente (`latest`).
* **Deploy no Servidor**: O servidor produtivo de monitoramento monitora o Docker Hub; ao detectar a nova imagem publicada pelo GitHub Actions, o servidor baixa a versão atualizada e reinicia a aplicação em produção de forma totalmente automatizada.
