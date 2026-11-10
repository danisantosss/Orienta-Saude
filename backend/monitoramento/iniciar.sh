#!/bin/sh
# Inicia o backend. Com APPLICATIONINSIGHTS_CONNECTION_STRING definida (só na Azure), liga o agente
# do Application Insights, que envia requisições, erros, chamadas ao banco e ao Gemini, logs e métricas.
# Sem ela (local, testes), a aplicação sobe normalmente, sem o agente.
if [ -n "$APPLICATIONINSIGHTS_CONNECTION_STRING" ]; then
  JAVA_OPTS="$JAVA_OPTS -javaagent:/app/agente/applicationinsights-agent.jar"
fi
exec java $JAVA_OPTS -jar /app/app.jar
