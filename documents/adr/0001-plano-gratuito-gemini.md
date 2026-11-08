# ADR 0001: Uso do plano gratuito da API Gemini

- **Status:** aceita
- **Data:** 06/11/2026

## Contexto

O Orienta Saúde usa um modelo de linguagem para conduzir a entrevista adaptativa e gerar a análise final da triagem, e um modelo de embeddings para a busca semântica do RAG. O projeto é acadêmico, sem orçamento, e precisa funcionar em produção na Azure durante a avaliação e as sessões de validação com a comunidade.

Os dados tratados são de saúde, que a LGPD classifica como dados pessoais sensíveis (art. 5º, II e art. 11).

## Opções avaliadas

| Opção | Custo | Ponto fraco |
|---|---|---|
| API Gemini, plano gratuito | Zero | O Google pode usar o conteúdo enviado para melhorar seus produtos; limites de requisições por minuto e por dia |
| API Gemini, plano pago | Por uso | O conteúdo não é usado para treino, mas exige cartão de crédito e gera custo |
| OpenAI ou Anthropic | Por uso | Mesmo problema de custo |
| Modelo aberto rodando no próprio servidor | Infraestrutura | Precisa de GPU ou de muita memória, o que não cabe na Azure for Students |

## Decisão

Usar o **plano gratuito da API Gemini** (`gemini-3.5-flash-lite` para texto e `gemini-embedding-001` para embeddings), com as seguintes proteções:

1. **Minimização**: o prompt leva apenas idade, sintomas e respostas da entrevista. Nome, email, id do usuário e qualquer outro identificador da conta nunca são enviados (o `PromptBuilder` é testado para isso).
2. **Transparência**: a Política de Privacidade informa que o texto anonimizado da triagem é processado pelo Google e que, no plano gratuito, ele pode ser usado para melhorar os produtos do Google. Também orienta o usuário a não escrever dados que o identifiquem nos sintomas.
3. **Consentimento**: o cadastro só é concluído com o aceite explícito dessa política (LGPD, art. 11, I).
4. **Chave fora do código**: a chave fica em variável de ambiente (`GEMINI_API_KEY`), guardada como *secret* na Azure.
5. **Limites de uso**: timeout de 10 s por chamada e no máximo 10 perguntas por entrevista, o que mantém o consumo dentro da cota gratuita.

## Consequências

- O projeto roda sem custo, mas depende da cota do plano gratuito; quando ela acaba, a API responde 503 e o usuário vê uma mensagem para tentar mais tarde.
- O texto dos sintomas sai do sistema e é processado pelo Google. Como não vai junto nenhum identificador da conta, o risco é reduzido, mas não eliminado (o usuário pode escrever algo identificável), por isso o aviso na política.
- Se o projeto for usado fora do contexto acadêmico, a recomendação é migrar para o plano pago, em que o conteúdo não é usado para treino. A troca é só de configuração (chave e cobrança), sem mudar o código.
