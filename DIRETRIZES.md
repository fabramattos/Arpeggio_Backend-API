# Diretrizes para Inteligências Artificiais e Assistentes de Desenvolvimento

Este arquivo define as regras e diretrizes obrigatórias que qualquer Inteligência Artificial (LLMs, assistentes de codificação, agentes autônomos) deve seguir estritamente ao atuar no desenvolvimento, manutenção ou refatoração do código do projeto **Arpeggio Backend**.

---

## 1. Imutabilidade das Diretrizes

* **Regra Primária**: A IA **NÃO DEVE** alterar, editar, remover ou reescrever este arquivo de diretrizes (`DIRETRIZES.md`) sob nenhuma circunstância, **exceto se houver uma solicitação explícita e direta do desenvolvedor (usuário)** pedindo para atualizar ou adicionar novas regras a este documento.

---

## 2. Controle de Versão e Visibilidade na IDE

* **Adição Obrigatória de Arquivos ao Git**: Todo e qualquer novo arquivo criado pela IA (seja classe Kotlin, arquivo de configuração, script ou documentação) **DEVE ser imediatamente adicionado ao Git** utilizando o comando de terminal (`git add <caminho_do_arquivo>`).
* **Motivo**: Isso garante que a IDE do desenvolvedor reconheça o arquivo imediatamente, visualize os diffs adequadamente e indexe as alterações sem deixar arquivos como *untracked* ou ocultos na área de trabalho do desenvolvedor.

---

## 3. Padrões de Arquitetura e Configuração

* **Segredos e Credenciais**: **NUNCA** adicione chaves de API, senhas, tokens ou quaisquer segredos em arquivos versionados (como `application.yml`, `application-dev.yml` ou `Docker-Compose.yml`). Segredos devem ser lidos por meio de variáveis de ambiente e injetados via `.env` local.
* **Perfis de Ambiente**:
  * Não altere a configuração base do `application.yml` em relação a produção sem alinhamento com o dev.
  * Lembre-se de que o ambiente local de conteinerização (Docker Compose) utiliza a variável `SPRING_PROFILES_ACTIVE=dev` para forçar o perfil local.
* **Pipelines e Build Docker**:
  * Não adicione rotinas de push para o Docker Hub no `Docker-Compose.yml` ou em scripts locais. A responsabilidade de realizar build oficial e push da imagem Docker pertence exclusivamente às pipelines de CI/CD do GitHub Actions (`.github/workflows/`).

---

## 4. Padrões de Código Kotlin e Spring Boot

* **Conformidade com Estilo Existente**: Siga rigorosamente as convenções de código Kotlin já existentes no projeto (orientação a objetos/funcional, imutabilidade com `val` sempre que possível, uso adequado de coroutines ou reatividade quando aplicável).
* **Integridade de Documentação**: Mantenha e preserve todos os comentários e docstrings existentes que não estejam diretamente relacionados a uma alteração de código solicitada, exceto se instruído em contrário pelo usuário.
* **Testes Automatizados**: Sempre que alterar lógica de negócios (services, controllers ou integrações), verifique se há impacto nos testes unitários e de integração na pasta `src/test/` e execute-os ou atualize-os de acordo com a necessidade.
