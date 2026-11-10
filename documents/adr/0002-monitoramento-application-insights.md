# ADR 0002: Monitoramento em produção com o Application Insights

- **Status:** aceita
- **Data:** 09/11/2026

## Contexto

O backend já expõe métricas no formato do Prometheus (`/actuator/prometheus`, Spring Boot Actuator + Micrometer), e o `docker-compose` sobe Prometheus e Grafana para uso local. Na Azure, porém, ninguém coletava essas métricas: o backend tem ingress interno e escala até zero réplicas, e os únicos dados de produção eram os logs dos contêineres no Log Analytics, sem painel nem alerta.

Era preciso saber, em produção, quantas requisições falham, quanto tempo as rotas e as chamadas ao Gemini levam, quais exceções acontecem e ser avisado quando algo quebra. Tudo sem custo além do crédito da Azure for Students e com o menor número possível de peças novas.

## Opções avaliadas

| Opção | Custo | Ponto fraco |
|---|---|---|
| Prometheus + Grafana como Container Apps no mesmo ambiente | Consome o crédito o tempo todo (os dois ficam sempre ligados) | Mais dois contêineres para manter, disco persistente para o Prometheus e um Grafana exposto na internet com senha |
| Grafana Cloud (plano gratuito) | Zero | Precisa de um coletor (Grafana Alloy) rodando dentro da Azure para alcançar o backend interno; os dados saem da Azure para outro fornecedor |
| **Azure Monitor / Application Insights com o agente Java** | 5 GB/mês de ingestão gratuitos; alertas de métrica custam centavos do crédito | Fica preso à Azure; o agente aumenta um pouco o tempo de inicialização e o uso de memória |

## Decisão

Usar o **Application Insights** (baseado no workspace do Log Analytics que o ambiente do Container Apps já usa), com o **agente Java 3.x**:

1. O agente é baixado pelo Maven (`maven-dependency-plugin`, versão fixa no `pom.xml`) e vai na imagem Docker. O `iniciar.sh` só o liga quando existe a variável `APPLICATIONINSIGHTS_CONNECTION_STRING`; local e nos testes ele fica desligado e o Prometheus/Grafana do `docker-compose` continua valendo.
2. Sem mudar o código, o agente coleta requisições, exceções, chamadas ao MySQL e ao Gemini, logs (INFO ou mais grave) e todas as métricas do Micrometer, inclusive a métrica de negócio `orienta.triagens.concluidas` (por nível de urgência).
3. As requisições do `/actuator` são descartadas (amostragem 0%) para não gastar a cota, e respostas 4xx (login errado, token expirado) não contam como falha do servidor (`captureHttpServer4xxAsError: false`); só 5xx dispara o alerta de erros.
4. Um limite diário de ingestão no workspace (0,15 GB/dia) impede gastar além da faixa gratuita.
5. Três alertas de métrica em produção mandam e-mail: requisições com erro, falhas em dependências externas e reinícios do contêiner.
6. Não há teste de disponibilidade (ping periódico): ele manteria o backend sempre ligado e acabaria com a escala a zero, que é o que mantém o projeto dentro do crédito.

## Consequências

- A telemetria fica na mesma região dos dados (`mexicocentral`) e na mesma conta. O agente não captura corpo de requisição nem cabeçalhos, e as consultas SQL vão com os valores mascarados; os sintomas ficam cifrados no banco e não aparecem em logs.
- A configuração da Azure é feita uma vez pelo `infra/azure/configurar-monitoramento.sh`; as consultas prontas estão em `infra/azure/consultas-monitoramento.kql`.
- Se o projeto sair da Azure, basta não definir a variável: o backend continua expondo `/actuator/prometheus` para qualquer coletor compatível.
