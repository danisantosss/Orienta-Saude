#!/usr/bin/env bash
# Cria a infraestrutura do Orienta Saúde na Azure (homologação e produção).
# Rodar uma vez no Azure Cloud Shell (Bash), já logado na assinatura Azure for Students:
#   GEMINI_API_KEY=... bash infra/azure/criar-infra.sh
# Pode ser executado de novo: o que já existe é mantido.
set -euo pipefail

# A conta Azure for Students só oferece o MySQL Flexible no México entre as regiões permitidas
LOCAL="${LOCAL:-mexicocentral}"
GRUPO="${GRUPO:-rg-orienta-saude}"
REPOSITORIO="${REPOSITORIO:-danisantosss/Orienta-Saude}"
AMBIENTE_CONTAINERS="cae-orienta-saude"
IDENTIDADE="id-orienta-saude-github"
USUARIO_BANCO="orientaadmin"

ASSINATURA=$(az account show --query id -o tsv)
TENANT=$(az account show --query tenantId -o tsv)
# O nome do servidor MySQL precisa ser único na Azure inteira
SERVIDOR_MYSQL="mysql-orienta-saude-${ASSINATURA:0:6}"

if [ -z "${GEMINI_API_KEY:-}" ]; then
  read -rsp "Chave da API do Gemini: " GEMINI_API_KEY
  echo
fi

echo ">> Registrando os provedores de recursos"
for provedor in Microsoft.App Microsoft.OperationalInsights Microsoft.DBforMySQL Microsoft.ManagedIdentity; do
  az provider register --namespace "$provedor" --wait
done
az extension add --name containerapp --upgrade --only-show-errors

echo ">> Grupo de recursos $GRUPO ($LOCAL)"
az group create --name "$GRUPO" --location "$LOCAL" --output none

echo ">> Banco MySQL $SERVIDOR_MYSQL"
if az mysql flexible-server show --resource-group "$GRUPO" --name "$SERVIDOR_MYSQL" --output none 2>/dev/null; then
  if [ -z "${SENHA_BANCO:-}" ]; then
    read -rsp "O servidor já existe. Senha do usuário $USUARIO_BANCO: " SENHA_BANCO
    echo
  fi
else
  SENHA_BANCO="Os1-$(openssl rand -hex 16)"
  # B1ms (Burstable) é o tamanho coberto pela oferta gratuita; 0.0.0.0 libera o acesso só para serviços da Azure
  az mysql flexible-server create \
    --resource-group "$GRUPO" --name "$SERVIDOR_MYSQL" --location "$LOCAL" \
    --admin-user "$USUARIO_BANCO" --admin-password "$SENHA_BANCO" \
    --tier Burstable --sku-name Standard_B1ms --storage-size 32 --version 8.0.21 \
    --public-access 0.0.0.0 --yes --output none
fi
for banco in orienta_saude_homolog orienta_saude_prod; do
  az mysql flexible-server db create --resource-group "$GRUPO" --server-name "$SERVIDOR_MYSQL" \
    --database-name "$banco" --charset utf8mb4 --collation utf8mb4_unicode_ci --output none
done

echo ">> Ambiente do Container Apps $AMBIENTE_CONTAINERS"
if ! az containerapp env show --resource-group "$GRUPO" --name "$AMBIENTE_CONTAINERS" --output none 2>/dev/null; then
  az containerapp env create --resource-group "$GRUPO" --name "$AMBIENTE_CONTAINERS" --location "$LOCAL" --output none
fi

# Imagem provisória: o primeiro deploy pelo GitHub Actions troca pela imagem real
IMAGEM_INICIAL="mcr.microsoft.com/k8se/quickstart:latest"

criar_aplicacoes() {
  local sufixo="$1" banco="$2"
  local backend="ca-orienta-backend-$sufixo" frontend="ca-orienta-frontend-$sufixo"

  echo ">> Backend $backend"
  if ! az containerapp show --resource-group "$GRUPO" --name "$backend" --output none 2>/dev/null; then
    # Ingress interno: o backend não é acessível pela internet, só pelo frontend
    az containerapp create --resource-group "$GRUPO" --name "$backend" \
      --environment "$AMBIENTE_CONTAINERS" --image "$IMAGEM_INICIAL" \
      --ingress internal --target-port 8080 \
      --cpu 0.5 --memory 1.0Gi --min-replicas 0 --max-replicas 1 \
      --secrets "db-password=$SENHA_BANCO" "jwt-secret=$(openssl rand -hex 48)" "gemini-api-key=$GEMINI_API_KEY" \
      --env-vars \
        SPRING_PROFILES_ACTIVE=prod \
        "DB_URL=jdbc:mysql://$SERVIDOR_MYSQL.mysql.database.azure.com:3306/$banco?sslMode=REQUIRED&serverTimezone=UTC" \
        "DB_USERNAME=$USUARIO_BANCO" \
        DB_PASSWORD=secretref:db-password \
        JWT_SECRET=secretref:jwt-secret \
        GEMINI_API_KEY=secretref:gemini-api-key \
      --output none
  fi
  local fqdn_backend
  fqdn_backend=$(az containerapp show --resource-group "$GRUPO" --name "$backend" \
    --query properties.configuration.ingress.fqdn -o tsv)

  echo ">> Frontend $frontend"
  if ! az containerapp show --resource-group "$GRUPO" --name "$frontend" --output none 2>/dev/null; then
    az containerapp create --resource-group "$GRUPO" --name "$frontend" \
      --environment "$AMBIENTE_CONTAINERS" --image "$IMAGEM_INICIAL" \
      --ingress external --target-port 80 \
      --cpu 0.25 --memory 0.5Gi --min-replicas 0 --max-replicas 1 \
      --env-vars "BACKEND_URL=https://$fqdn_backend" \
      --output none
  fi
}

criar_aplicacoes homolog orienta_saude_homolog
criar_aplicacoes prod orienta_saude_prod

echo ">> Identidade usada pelo GitHub Actions (login sem senha via OIDC)"
az identity create --resource-group "$GRUPO" --name "$IDENTIDADE" --output none
ID_CLIENTE=$(az identity show --resource-group "$GRUPO" --name "$IDENTIDADE" --query clientId -o tsv)
ID_PRINCIPAL=$(az identity show --resource-group "$GRUPO" --name "$IDENTIDADE" --query principalId -o tsv)

# Só os jobs que rodam nos environments homologacao e producao do repositório recebem o token
for ambiente in homologacao producao; do
  az identity federated-credential create --resource-group "$GRUPO" --identity-name "$IDENTIDADE" \
    --name "github-$ambiente" \
    --issuer "https://token.actions.githubusercontent.com" \
    --subject "repo:$REPOSITORIO:environment:$ambiente" \
    --audiences "api://AzureADTokenExchange" --output none
done

# Permissão limitada ao grupo de recursos do projeto
ESCOPO="/subscriptions/$ASSINATURA/resourceGroups/$GRUPO"
if [ -z "$(az role assignment list --assignee "$ID_PRINCIPAL" --scope "$ESCOPO" --role Contributor --query "[].id" -o tsv)" ]; then
  az role assignment create --assignee-object-id "$ID_PRINCIPAL" --assignee-principal-type ServicePrincipal \
    --role Contributor --scope "$ESCOPO" --output none
fi

URL_HOMOLOG=$(az containerapp show --resource-group "$GRUPO" --name ca-orienta-frontend-homolog --query properties.configuration.ingress.fqdn -o tsv)
URL_PROD=$(az containerapp show --resource-group "$GRUPO" --name ca-orienta-frontend-prod --query properties.configuration.ingress.fqdn -o tsv)

cat <<FIM

Pronto. Cadastre estes três secrets no GitHub (Settings > Secrets and variables > Actions):
  AZURE_CLIENT_ID       = $ID_CLIENTE
  AZURE_TENANT_ID       = $TENANT
  AZURE_SUBSCRIPTION_ID = $ASSINATURA

Homologação: https://$URL_HOMOLOG
Produção:    https://$URL_PROD
FIM
