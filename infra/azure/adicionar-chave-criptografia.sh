#!/usr/bin/env bash
# Cria a chave que cifra os dados de saúde no banco (APP_CRIPTOGRAFIA_CHAVE) nos backends
# que já existem na Azure. Rodar uma vez no Cloud Shell, antes do deploy que traz a criptografia.
# Se a chave já existir em um backend, ele é pulado: trocar a chave deixaria os dados antigos ilegíveis.
set -euo pipefail

GRUPO="${GRUPO:-rg-orienta-saude}"

for sufixo in homolog prod; do
  backend="ca-orienta-backend-$sufixo"
  if az containerapp secret list --resource-group "$GRUPO" --name "$backend" --query "[].name" -o tsv \
      | grep -qx "criptografia-chave"; then
    echo ">> $backend já tem a chave, nada a fazer"
    continue
  fi
  echo ">> Criando a chave em $backend"
  az containerapp secret set --resource-group "$GRUPO" --name "$backend" \
    --secrets "criptografia-chave=$(openssl rand -base64 32)" --output none
  az containerapp update --resource-group "$GRUPO" --name "$backend" \
    --set-env-vars APP_CRIPTOGRAFIA_CHAVE=secretref:criptografia-chave --output none
done

echo ">> Pronto. Guarde uma cópia das chaves em local seguro: sem elas os dados cifrados não podem ser lidos."
echo "   Para ver: az containerapp secret show -g $GRUPO -n ca-orienta-backend-prod --secret-name criptografia-chave"
