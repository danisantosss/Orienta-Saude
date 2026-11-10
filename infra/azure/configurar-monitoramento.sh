#!/usr/bin/env bash
# Liga o monitoramento do Orienta Saúde na Azure (Application Insights + alertas por e-mail).
# Rodar uma vez no Azure Cloud Shell (Bash), depois do criar-infra.sh:
#   EMAIL_ALERTA=voce@exemplo.com bash infra/azure/configurar-monitoramento.sh
# Pode ser executado de novo: o que já existe é mantido.
set -euo pipefail

LOCAL="${LOCAL:-mexicocentral}"
GRUPO="${GRUPO:-rg-orienta-saude}"
AMBIENTE_CONTAINERS="cae-orienta-saude"
GRUPO_ACOES="ag-orienta-saude"
# Limite diário de ingestão do Log Analytics (GB). 0,15 GB/dia fica abaixo dos 5 GB/mês gratuitos
LIMITE_DIARIO_GB="${LIMITE_DIARIO_GB:-0.15}"

if [ -z "${EMAIL_ALERTA:-}" ]; then
  read -rp "E-mail que recebe os alertas de produção: " EMAIL_ALERTA
fi

echo ">> Registrando o provedor do Azure Monitor"
az provider register --namespace Microsoft.Insights --wait
az extension add --name application-insights --upgrade --only-show-errors
az extension add --name containerapp --upgrade --only-show-errors

# O ambiente do Container Apps já manda os logs dos contêineres para um Log Analytics.
# O Application Insights usa o mesmo workspace: logs e telemetria ficam no mesmo lugar (e na mesma região).
echo ">> Workspace do Log Analytics usado pelo ambiente $AMBIENTE_CONTAINERS"
ID_CLIENTE_WORKSPACE=$(az containerapp env show --resource-group "$GRUPO" --name "$AMBIENTE_CONTAINERS" \
  --query properties.appLogsConfiguration.logAnalyticsConfiguration.customerId -o tsv)
WORKSPACE=$(az monitor log-analytics workspace list --resource-group "$GRUPO" \
  --query "[?customerId=='$ID_CLIENTE_WORKSPACE'].id | [0]" -o tsv)
if [ -z "$WORKSPACE" ]; then
  echo "Não encontrei o workspace do ambiente no grupo $GRUPO." >&2
  exit 1
fi
# Trava de custo: passando do limite no dia, a coleta para até o dia seguinte
az monitor log-analytics workspace update --ids "$WORKSPACE" --quota "$LIMITE_DIARIO_GB" --output none

ligar_application_insights() {
  local sufixo="$1"
  local recurso="appi-orienta-saude-$sufixo" backend="ca-orienta-backend-$sufixo"

  echo ">> Application Insights $recurso"
  if ! az monitor app-insights component show --resource-group "$GRUPO" --app "$recurso" --output none 2>/dev/null; then
    az monitor app-insights component create --resource-group "$GRUPO" --app "$recurso" \
      --location "$LOCAL" --kind web --application-type web --workspace "$WORKSPACE" --output none
  fi
  local conexao
  conexao=$(az monitor app-insights component show --resource-group "$GRUPO" --app "$recurso" \
    --query connectionString -o tsv)

  # Com a variável definida, o backend liga o agente do Application Insights ao iniciar (monitoramento/iniciar.sh)
  echo ">> Ligando o agente em $backend"
  az containerapp secret set --resource-group "$GRUPO" --name "$backend" \
    --secrets "appinsights-conexao=$conexao" --output none
  az containerapp update --resource-group "$GRUPO" --name "$backend" \
    --set-env-vars APPLICATIONINSIGHTS_CONNECTION_STRING=secretref:appinsights-conexao --output none
}

ligar_application_insights homolog
ligar_application_insights prod

echo ">> Grupo de ações $GRUPO_ACOES (e-mail para $EMAIL_ALERTA)"
if ! az monitor action-group show --resource-group "$GRUPO" --name "$GRUPO_ACOES" --output none 2>/dev/null; then
  az monitor action-group create --resource-group "$GRUPO" --name "$GRUPO_ACOES" --short-name orienta \
    --action email responsavel "$EMAIL_ALERTA" --output none
fi

criar_alerta() {
  local nome="$1" escopo="$2" condicao="$3" janela="$4" descricao="$5"
  if az monitor metrics alert show --resource-group "$GRUPO" --name "$nome" --output none 2>/dev/null; then
    echo ">> Alerta $nome já existe"
    return
  fi
  echo ">> Alerta $nome"
  az monitor metrics alert create --resource-group "$GRUPO" --name "$nome" --scopes "$escopo" \
    --condition "$condicao" --window-size "$janela" --evaluation-frequency 5m --severity 2 \
    --action "$GRUPO_ACOES" --description "$descricao" --output none
}

# Alertas só de produção: homologação quebra de propósito durante os testes
APPI_PROD=$(az monitor app-insights component show --resource-group "$GRUPO" --app appi-orienta-saude-prod --query id -o tsv)
BACKEND_PROD=$(az containerapp show --resource-group "$GRUPO" --name ca-orienta-backend-prod --query id -o tsv)

criar_alerta alerta-erros-backend-prod "$APPI_PROD" "count requests/failed > 5" 5m \
  "Mais de 5 requisições com erro (5xx) no backend de produção em 5 minutos"
criar_alerta alerta-dependencias-prod "$APPI_PROD" "count dependencies/failed > 5" 5m \
  "Mais de 5 falhas em chamadas externas (Gemini ou banco) em 5 minutos"
criar_alerta alerta-reinicios-backend-prod "$BACKEND_PROD" "total RestartCount > 2" 15m \
  "O contêiner do backend de produção reiniciou mais de 2 vezes em 15 minutos"

cat <<FIM

Pronto. O agente começa a mandar dados na próxima vez que o backend iniciar (a revisão nova já sobe agora).
No portal: Grupo de recursos $GRUPO > appi-orienta-saude-prod > Mapa do aplicativo, Falhas, Desempenho,
Métricas ao vivo e Logs (consultas em infra/azure/consultas-monitoramento.kql).
FIM
