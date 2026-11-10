# Infraestrutura na Azure

O Orienta Saúde roda na Azure for Students com dois ambientes iguais:

| Ambiente | Branch | Container Apps | Banco |
|---|---|---|---|
| Homologação | `dev` | `ca-orienta-frontend-homolog`, `ca-orienta-backend-homolog` | `orienta_saude_homolog` |
| Produção | `main` | `ca-orienta-frontend-prod`, `ca-orienta-backend-prod` | `orienta_saude_prod` |

- **Azure Container Apps**: roda as imagens Docker do frontend (nginx) e do backend (Spring Boot), com HTTPS automático e escala até zero réplicas quando ninguém usa.
- **Azure Database for MySQL Flexible Server** (B1ms): um servidor com um banco para cada ambiente.
- O backend tem ingress **interno**: só o nginx do frontend chega até ele. A internet vê apenas o frontend.
- Os segredos (senha do banco, `JWT_SECRET`, `GEMINI_API_KEY`, `APP_CRIPTOGRAFIA_CHAVE`) ficam como *secrets* dos Container Apps, nunca no repositório.
- As imagens ficam no GitHub Container Registry (`ghcr.io/danisantosss/orienta-saude-backend` e `-frontend`).

## Criar a infraestrutura (uma vez)

1. No [portal da Azure](https://portal.azure.com), abra o **Cloud Shell** em modo Bash.
2. Rode:
   ```bash
   git clone -b dev https://github.com/danisantosss/Orienta-Saude.git
   cd Orienta-Saude
   bash infra/azure/criar-infra.sh
   ```
   O script pede a chave do Gemini e gera as demais senhas. A região padrão é `mexicocentral`: a assinatura Azure for Students só permite algumas regiões (`az policy assignment list`) e, entre elas, só o México oferece o MySQL Flexible Server. Para usar outra: `LOCAL=<regiao> bash infra/azure/criar-infra.sh`.
3. No GitHub, em *Settings > Secrets and variables > Actions*, cadastre os três valores que o script imprime: `AZURE_CLIENT_ID`, `AZURE_TENANT_ID` e `AZURE_SUBSCRIPTION_ID`.
4. Em *Packages* do perfil no GitHub, deixe `orienta-saude-backend` e `orienta-saude-frontend` como **Public** (depois do primeiro deploy, que cria os pacotes), para a Azure conseguir baixar as imagens sem senha.

## Monitoramento (uma vez)

Depois do `criar-infra.sh`, rode no Cloud Shell:

```bash
EMAIL_ALERTA=seu@email.com bash infra/azure/configurar-monitoramento.sh
```

O script:

- cria um **Application Insights** para cada ambiente (`appi-orienta-saude-homolog` e `appi-orienta-saude-prod`) no mesmo Log Analytics que já recebe os logs dos contêineres;
- grava a connection string como *secret* do backend e define `APPLICATIONINSIGHTS_CONNECTION_STRING`, o que liga o agente Java na inicialização;
- limita a ingestão do Log Analytics a 0,15 GB por dia, abaixo dos 5 GB mensais gratuitos;
- cria o grupo de ações `ag-orienta-saude` (e-mail) e três alertas de produção: mais de 5 requisições com erro em 5 min, mais de 5 falhas em chamadas externas (Gemini ou banco) em 5 min e mais de 2 reinícios do backend em 15 min.

Para ver os dados: portal > `rg-orienta-saude` > `appi-orienta-saude-prod`. Os painéis prontos são **Mapa do aplicativo**, **Falhas**, **Desempenho** e **Métricas ao vivo**; em **Logs** dá para colar as consultas de [`consultas-monitoramento.kql`](./consultas-monitoramento.kql), como triagens por nível de urgência e falhas do Gemini. Não há teste de disponibilidade periódico de propósito: ele deixaria o backend sempre ligado e acabaria com a escala a zero.

## Chave de criptografia (bancos criados antes dela)

Os dados de saúde são cifrados no banco com a chave `APP_CRIPTOGRAFIA_CHAVE`, obrigatória no profile `prod`. O `criar-infra.sh` já cria essa chave; para os backends que existiam antes, rode uma vez no Cloud Shell, **antes** do deploy que traz a criptografia:

```bash
bash infra/azure/adicionar-chave-criptografia.sh
```

O script pula o backend que já tem a chave. Nunca troque nem apague essa chave: sem ela, os sintomas e resultados já gravados não podem mais ser lidos. Os dados gravados antes da criptografia continuam legíveis e passam a ser cifrados quando forem regravados.

## Como o deploy funciona

- `push` em `dev` → `deploy-homologacao.yml`: roda os testes (`ci.yml`), publica as imagens e atualiza os Container Apps de homologação.
- `push` em `main` (merge do PR) → `deploy-producao.yml`: testes, imagens, Container Apps de produção e, no fim, a tag e a release `vX.Y.Z`.
- O GitHub Actions entra na Azure por OIDC com uma identidade gerenciada (`id-orienta-saude-github`) que só tem permissão no grupo `rg-orienta-saude`. Não existe senha da Azure guardada no GitHub, nem acesso por SSH ou FTP.

## Rodar localmente com Docker

```bash
cp .env.example .env   # preencha a GEMINI_API_KEY
docker compose up --build
```

Frontend em http://localhost:3000, backend em http://localhost:8080, Prometheus em http://localhost:9090 e Grafana em http://localhost:3001 (admin/admin).
