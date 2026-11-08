# 🩺 Orienta Saúde

Plataforma web de **triagem inicial de sintomas de saúde**. O usuário descreve o que está sentindo, responde a uma entrevista adaptativa conduzida por **IA generativa (Gemini)** e recebe, com apoio de **RAG (Retrieval-Augmented Generation)** sobre uma base de fontes públicas de saúde, orientações educativas e uma estimativa do nível de urgência: **Leve, Moderado, Urgente ou Emergência**.

> ⚠️ O Orienta Saúde **não realiza diagnóstico médico** e **não substitui avaliação médica presencial**. As orientações geradas têm caráter exclusivamente educativo.

Projeto desenvolvido como Trabalho de Conclusão de Curso (TCC) na Universidade Católica de Santa Catarina.

| Ambiente | Endereço |
|---|---|
| Produção | https://ca-orienta-frontend-prod.gentlehill-9074b666.mexicocentral.azurecontainerapps.io |
| Homologação | https://ca-orienta-frontend-homolog.gentlehill-9074b666.mexicocentral.azurecontainerapps.io |

> Os ambientes escalam até zero quando ninguém usa, então o primeiro acesso pode levar alguns segundos.

---

## 📋 Índice

- [Sobre o projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Stack tecnológica](#-stack-tecnológica)
- [Arquitetura](#-arquitetura)
- [Executar com Docker (recomendado)](#-executar-com-docker-recomendado)
- [Executar sem Docker](#-executar-sem-docker)
- [Variáveis de ambiente](#-variáveis-de-ambiente)
- [Testes e cobertura](#-testes-e-cobertura)
- [CI/CD e ambientes](#-cicd-e-ambientes)
- [Monitoramento](#-monitoramento)
- [Estrutura do repositório](#-estrutura-do-repositório)
- [Documentação completa (RFC)](#-documentação-completa-rfc)
- [Roadmap](#-roadmap)
- [Privacidade e LGPD](#-privacidade-e-lgpd)
- [Autor](#-autor)
- [Licença](#-licença)

---

## 📖 Sobre o projeto

O acesso inicial a orientação em saúde ainda é um desafio: muitas pessoas recorrem à internet ao sentir sintomas e ficam confusas com informações genéricas, sem saber se devem procurar atendimento imediato ou aguardar.

O **Orienta Saúde** propõe uma abordagem direta: em vez de listar dezenas de condições possíveis, o foco é **classificar o nível de urgência** e fornecer orientação educativa clara, ajudando o usuário a decidir o próximo passo.

## ✨ Funcionalidades

- 🔐 Cadastro e login de usuários (autenticação via JWT)
- 💬 Entrevista adaptativa: as perguntas são geradas pela IA com base nas respostas anteriores
- 📚 Recuperação de conhecimento via RAG, com busca por embeddings e busca por palavras-chave como alternativa quando os embeddings não estão disponíveis
- 🚦 Classificação de urgência em 4 níveis: **Leve · Moderado · Urgente · Emergência**
- 📝 Orientações educativas, sinais de alerta e recomendações, sem diagnóstico nem prescrição (as respostas da IA passam por uma validação ética antes de chegar ao usuário)
- 🕘 Histórico de triagens anteriores, com filtros e paginação
- 🛡 Nome e e-mail do usuário nunca são enviados ao modelo de IA
- 🔏 LGPD: consentimento no cadastro, política de privacidade, perfil com os dados da conta, exclusão da conta e de todas as triagens, e dados de saúde criptografados no banco (AES-256-GCM)
- 📱 Interface responsiva

## 🛠 Stack tecnológica

| Camada | Tecnologia |
|---|---|
| Backend | Java 17 + Spring Boot 3.3 (Spring Web, Spring Security, Spring Data JPA, Actuator) |
| Frontend | React 18 + Vite 5 + TypeScript |
| Banco de dados | MySQL 8 |
| IA generativa | Gemini API (`gemini-3.5-flash-lite` para texto, `gemini-embedding-001` para embeddings) |
| RAG | Base de conhecimento em JSON (`backend/src/main/resources/knowledge-base/`), indexada por embeddings |
| Testes | JUnit 5 + Testcontainers (MySQL) + JaCoCo · Vitest + Testing Library |
| Qualidade | ESLint · SonarCloud |
| Infraestrutura | Docker, GitHub Actions, GitHub Container Registry, Azure Container Apps, Azure Database for MySQL |
| Monitoramento | Spring Boot Actuator + Micrometer, Prometheus e Grafana |

## 🏗 Arquitetura

O backend segue arquitetura em camadas (`controller` → `service` → `repository`), com módulos próprios para a IA (`ia/`: cliente do Gemini, montagem de prompts e validação ética) e para o RAG (`rag/`: base de conhecimento, índice de embeddings e busca). O frontend é uma SPA servida pelo nginx, que também repassa as chamadas `/api` para o backend.

Principais rotas da API:

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/registro` | Cadastro de usuário |
| POST | `/api/auth/login` | Login (retorna o JWT) |
| POST | `/api/triagem/iniciar` | Inicia uma triagem com a queixa principal |
| POST | `/api/triagem/{id}/mensagem` | Envia uma resposta da entrevista |
| POST | `/api/triagem/{id}/encerrar` | Encerra a entrevista |
| POST | `/api/triagem/{id}/analisar` | Gera a análise final (urgência e orientações) |
| GET | `/api/triagem/{id}/resultado` | Consulta o resultado de uma triagem |
| GET | `/api/triagem/historico` | Lista as triagens do usuário |

Diagramas C4, DER e de sequência estão no [RFC](./documents/RFC.md).

## 🐳 Executar com Docker (recomendado)

Pré-requisitos: [Docker](https://docs.docker.com/get-docker/) com Docker Compose e uma chave da [API do Gemini](https://aistudio.google.com/apikey).

```bash
git clone https://github.com/danisantosss/Orienta-Saude.git
cd Orienta-Saude
cp .env.example .env    # preencha GEMINI_API_KEY no .env
docker compose up --build
```

| Serviço | Endereço |
|---|---|
| Site (frontend) | http://localhost:3000 |
| API (backend) | http://localhost:8080 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (usuário `admin`, senha `admin` ou o valor de `GRAFANA_PASSWORD`) |
| MySQL | localhost:3306 (usuário `root`, senha `root` ou o valor de `DB_PASSWORD`) |

Para parar: `docker compose down` (os dados do MySQL ficam no volume `dados-mysql`; use `docker compose down -v` para apagá-los).

## 💻 Executar sem Docker

Pré-requisitos: [Java 17](https://adoptium.net/), [Maven 3.9+](https://maven.apache.org/), [Node.js 22](https://nodejs.org/) e [MySQL 8](https://dev.mysql.com/downloads/) rodando em `localhost:3306`.

### Backend

```bash
cd backend
export GEMINI_API_KEY=sua-chave
export DB_USERNAME=root      # opcional, padrão root
export DB_PASSWORD=root      # opcional, padrão root
mvn spring-boot:run
```

O backend sobe em http://localhost:8080 com o profile `dev`. O banco `orienta_saude` é criado automaticamente na primeira execução e as tabelas são geradas a partir das entidades.

### Frontend

```bash
cd frontend
npm install
echo "VITE_API_URL=" > .env.local
npm run dev
```

O site sobe em http://localhost:3000. Com `VITE_API_URL` vazio, o frontend chama `/api` no próprio endereço e o servidor do Vite repassa as requisições para o backend em `localhost:8080`.

## 🔑 Variáveis de ambiente

No Docker Compose, os valores vêm do arquivo `.env` na raiz (modelo em [`.env.example`](./.env.example)):

| Variável | Uso | Padrão |
|---|---|---|
| `GEMINI_API_KEY` | Chave da API do Gemini (obrigatória para a triagem) | vazio |
| `DB_PASSWORD` | Senha do usuário `root` do MySQL | `root` |
| `JWT_SECRET` | Chave de assinatura dos tokens JWT (mínimo 256 bits) | chave de desenvolvimento |
| `APP_CRIPTOGRAFIA_CHAVE` | Chave AES-256 em Base64 que cifra os dados de saúde no banco (`openssl rand -base64 32`). Obrigatória no profile `prod` | chave de desenvolvimento |
| `GRAFANA_PASSWORD` | Senha do usuário `admin` do Grafana | `admin` |

Outras variáveis lidas pelo backend:

| Variável | Uso |
|---|---|
| `DB_USERNAME` | Usuário do banco (padrão `root` no profile `dev`) |
| `DB_URL` | URL JDBC do banco (obrigatória no profile `prod`) |
| `SPRING_PROFILES_ACTIVE` | `dev` (padrão) ou `prod` |
| `GEMINI_MODELO_TEXTO` | Modelo de texto (padrão `gemini-3.5-flash-lite`) |
| `GEMINI_MODELO_EMBEDDING` | Modelo de embeddings (padrão `gemini-embedding-001`) |
| `JPA_DDL_AUTO` | Estratégia do Hibernate no profile `prod` (padrão `update`) |

No frontend, `VITE_API_URL` define o endereço da API. Sem ela, o padrão é `http://localhost:8080`; vazia, as chamadas vão para `/api` no mesmo endereço do site (como no Docker e na Azure).

## 🧪 Testes e cobertura

O projeto é desenvolvido com TDD. Metas de cobertura de linhas: **75% no backend** e **25% no frontend**, verificadas automaticamente (o build falha abaixo disso).

```bash
# Backend: testes + JaCoCo (precisa do Docker rodando, pois o Testcontainers sobe um MySQL 8)
cd backend
mvn verify
# relatório em backend/target/site/jacoco/index.html

# Frontend
cd frontend
npm run lint
npm test                 # roda os testes uma vez
npm run test:coverage    # testes + cobertura (relatório em frontend/coverage/)
```

## 🔄 CI/CD e ambientes

Os workflows ficam em [`.github/workflows/`](./.github/workflows):

| Workflow | Quando roda | O que faz |
|---|---|---|
| `ci.yml` | Pull request para `main` (e chamado pelos deploys) | Testes e cobertura do backend, lint, build e testes do frontend e, nos PRs, o quality gate do SonarCloud |
| `deploy-homologacao.yml` | Push em `dev` | Roda o CI, publica as imagens no GHCR e atualiza os Container Apps de homologação |
| `deploy-producao.yml` | Push em `main` (merge do PR) | Roda o CI, publica as imagens, atualiza produção e cria a tag e a release `vX.Y.Z` |

Fluxo de branches: o desenvolvimento acontece em `dev` (que publica em homologação) e chega à `main` por pull request revisado (que publica em produção).

Os dois ambientes rodam na Azure (Azure for Students, região `mexicocentral`) em Azure Container Apps, com um Azure Database for MySQL Flexible Server com um banco por ambiente. O backend só é acessível pelo frontend; os segredos ficam como *secrets* dos Container Apps e o GitHub Actions entra na Azure por OIDC, sem senha guardada no GitHub. A criação da infraestrutura está descrita em [`infra/azure/README.md`](./infra/azure/README.md).

| Ambiente | Branch | Endereço |
|---|---|---|
| Homologação | `dev` | https://ca-orienta-frontend-homolog.gentlehill-9074b666.mexicocentral.azurecontainerapps.io |
| Produção | `main` | https://ca-orienta-frontend-prod.gentlehill-9074b666.mexicocentral.azurecontainerapps.io |

## 📈 Monitoramento

O backend expõe `/actuator/health` e `/actuator/prometheus` (Spring Boot Actuator + Micrometer). No ambiente local do Docker Compose, o Prometheus ([`monitoramento/prometheus.yml`](./monitoramento/prometheus.yml)) coleta essas métricas e o Grafana já vem com o Prometheus configurado como fonte de dados.

## 📂 Estrutura do repositório

```
Orienta-Saude/
├── backend/            # API REST (Java 17 + Spring Boot)
├── frontend/           # Aplicação web (React + Vite + TypeScript) e config do nginx
├── infra/azure/        # Script e instruções da infraestrutura na Azure
├── monitoramento/      # Configuração do Prometheus e do Grafana
├── documents/          # RFC do TCC e ADRs (decisões de arquitetura)
├── images/             # Diagramas, mockups e gráficos usados na documentação
├── .github/workflows/  # CI e deploys
├── docker-compose.yml  # Ambiente local completo
└── .env.example        # Modelo das variáveis de ambiente
```

## 📄 Documentação completa (RFC)

O RFC completo (contexto, personas, requisitos funcionais e não funcionais, mockups, arquitetura C4 e cronograma) está em [`documents/RFC.md`](./documents/RFC.md).

## 🔏 Privacidade e LGPD

- **Consentimento**: o cadastro só é aceito com a caixa da Política de Privacidade marcada; a data e a versão aceita ficam gravadas.
- **Política de Privacidade e Termos de Uso**: página `/privacidade`, com link no cadastro e no rodapé.
- **Meu perfil** (`/perfil`): nome, email, data de cadastro e data do consentimento (`GET /api/usuario/perfil`).
- **Exclusão da conta**: `DELETE /api/usuario/conta` com a senha de confirmação apaga a conta, as triagens, as mensagens e os resultados; o login e os tokens antigos deixam de funcionar.
- **Criptografia em repouso**: sintomas, mensagens e textos do resultado são cifrados com AES-256-GCM antes de ir para o banco.
- **IA**: só idade, sintomas e respostas vão para a API Gemini. A decisão de usar o plano gratuito está em [`documents/adr/0001-plano-gratuito-gemini.md`](./documents/adr/0001-plano-gratuito-gemini.md).

## 🗺 Roadmap

| Marco | Descrição | Prazo |
|-------|-----------|-------|
| M1 | Finalização do RFC | Junho/2026 |
| M2 | Início do desenvolvimento | Julho/2026 |
| M3 | Autenticação, cadastro e triagem de sintomas | Agosto/2026 |
| M4 | Integração IA + RAG, histórico de triagens | Setembro/2026 |
| M5 | Integração dos módulos e refinamento de UX | Outubro/2026 |
| M6 | Testes funcionais e correções | Novembro/2026 |
| M7 | Entrega final do TCC | Dezembro/2026 |

## 👤 Autor

**Daniel Douglas dos Santos**
Projeto de TCC - Universidade Católica de Santa Catarina

## 📜 Licença

Distribuído sob a licença MIT. Veja [`LICENSE`](./LICENSE).

---

<sub>Este projeto é exclusivamente educativo e não substitui avaliação médica profissional.</sub>
