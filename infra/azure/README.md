# Infraestrutura na Azure

O Orienta Saúde roda na Azure for Students com dois ambientes iguais:

| Ambiente | Branch | Container Apps | Banco |
|---|---|---|---|
| Homologação | `dev` | `ca-orienta-frontend-homolog`, `ca-orienta-backend-homolog` | `orienta_saude_homolog` |
| Produção | `main` | `ca-orienta-frontend-prod`, `ca-orienta-backend-prod` | `orienta_saude_prod` |

- **Azure Container Apps**: roda as imagens Docker do frontend (nginx) e do backend (Spring Boot), com HTTPS automático e escala até zero réplicas quando ninguém usa.
- **Azure Database for MySQL Flexible Server** (B1ms): um servidor com um banco para cada ambiente.
- O backend tem ingress **interno**: só o nginx do frontend chega até ele. A internet vê apenas o frontend.
- Os segredos (senha do banco, `JWT_SECRET`, `GEMINI_API_KEY`) ficam como *secrets* dos Container Apps, nunca no repositório.
- As imagens ficam no GitHub Container Registry (`ghcr.io/danisantosss/orienta-saude-backend` e `-frontend`).

## Criar a infraestrutura (uma vez)

1. No [portal da Azure](https://portal.azure.com), abra o **Cloud Shell** em modo Bash.
2. Rode:
   ```bash
   git clone -b dev https://github.com/danisantosss/Orienta-Saude.git
   cd Orienta-Saude
   bash infra/azure/criar-infra.sh
   ```
   O script pede a chave do Gemini e gera as demais senhas. Se a região `brazilsouth` não for permitida na assinatura, rode com outra: `LOCAL=eastus bash infra/azure/criar-infra.sh`.
3. No GitHub, em *Settings > Secrets and variables > Actions*, cadastre os três valores que o script imprime: `AZURE_CLIENT_ID`, `AZURE_TENANT_ID` e `AZURE_SUBSCRIPTION_ID`.
4. Em *Packages* do perfil no GitHub, deixe `orienta-saude-backend` e `orienta-saude-frontend` como **Public** (depois do primeiro deploy, que cria os pacotes), para a Azure conseguir baixar as imagens sem senha.

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
