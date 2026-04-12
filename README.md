# RFC: Request for Comments — Projeto de Portfólio

## Título do Projeto:
Orienta Saúde  

## Linha de Projeto (Direction):
Plataforma Web 

## Autor:
Daniel Douglas dos Santos  

## Data da Proposta:
27/02/2026 

## Versão:
1.0 

---

## Visão do Produto e Impacto (O Problema)

### Contexto e Problema

O acesso inicial a orientação em saúde ainda é um desafio para grande parte da população. Muitas pessoas enfrentam dúvidas ao apresentar sintomas, sem saber se devem procurar atendimento imediato ou aguardar evolução do quadro. 

Esse problema afeta principalmente:

- Pessoas sem conhecimento médico;
- Indivíduos com acesso limitado a serviços de saúde;
- Usuários que recorrem à internet para autodiagnóstico.

Esse problema ocorre:

- Quando surgem sintomas inesperados;
- Na dúvida entre ir ao médico ou não;
- Durante buscas na internet por autodiagnóstico,

Atualmente, esse problema é resolvido de três formas principais:

- Busca em sites e blogs;
- Uso de aplicativos de saúde;
- Atendimento direto em unidades médicas.

Limitações das soluções atuais:

- Informações genéricas ou não confiáveis;
- Risco de interpretação errada pelo usuário;
- Sobrecarga em serviços de saúde por casos não urgentes.

---

## Origem da Demanda e Evidências

### Demanda Externa

A motivação inicial do projeto veio de relatos observados por uma profissional da área da saúde (estagiária em ambiente hospitalar), que identificou que muitos pacientes chegam às unidades de atendimento com dúvidas sobre sintomas que poderiam ser inicialmente classificados como leves ou moderados, gerando sobrecarga no atendimento e insegurança por parte dos próprios pacientes. 

A partir dessa observação, identificou-se a necessidade de uma ferramenta que auxiliasse usuários na compreensão inicial de sintomas, oferecendo orientação básica e indicação de nível de urgência, sem substituir avaliação médica profissional. 

### Pesquisa com usuários

Para validar essa necessidade, foi realizada uma pesquisa por meio de formulário online, com o objetivo de analisar o comportamento das pessoas ao lidar com sintomas de saúde. 

![Gráfico - Busca de sintomas](images/grafico1.jpeg)

Observa-se que a maioria dos participantes já buscou informações sobre sintomas na internet, indicando um comportamento comum entre os usuários. 

![Gráfico - Busca de sintomas](images/grafico2.jpeg)

Observa-se que o maior problema ao buscar sintomas na internet são a quantidade de possibilidades de doenças, causando um sentimento de confusão nas pessoas. 

![Gráfico - Busca de sintomas](images/grafico3.jpeg)

Mais de 90% dos participantes demonstraram interesse em uma plataforma para triagem inicial de sintomas. 

### Evidência de Interesse

Os resultados do questionário indicam um interesse real na proposta do sistema, especialmente no que se refere à necessidade de orientação inicial sobre sintomas de saúde. 

Além disso, observa-se que o comportamento de busca por informações médicas na internet é comum, porém frequentemente associado à insegurança e dificuldade de interpretação, reforçando a relevância da solução proposta. 

---

## Análise de Soluções Existentes (Benchmark)

### Ada Health

Link: https://ada.com/pt  
Público Alvo: Usuários gerais.  
Funcionalidades Principais: Avaliação de sintomas com IA e pesquisas científicas.  
Limitações: Respostas longas e complexas.

![Benchmark](images/ada.jpeg)

### WebMD

Link: https://www.webmd.com  
Público Alvo: Estudantes da área de medicina e usuários gerais.  
Funcionalidades Principais: Conteúdo médico e verificação de sintomas.  
Limitações: Foco em informação, não em triagem prática. 

![Benchmark](images/webmd.jpeg) 

### Symptomate

Link: https://symptomate.com/pt-br  
Público Alvo: Usuários de internet.  
Funcionalidades Principais: Avaliação de sintomas com IA.  
Limitações: Pode gerar múltiplas possibilidades sem indicar a urgência.

![Benchmark](images/symptomate.jpeg)

---

### Comparação

| Solução      | Pontos Fortes                                      | Limitações                                                                                          |
|-------------|----------------------------------------------------|------------------------------------------------------------------------------------------------------|
| Ada Health  | Possui ampla base de conhecimento médico           | Interface mais complexa e pode gerar respostas longas e pouco objetivas para usuários leigos        |
| WebMD       | Grande base de conteúdo médico e alta confiabilidade de informações | Foco em conteúdo informativo, sem classificação clara de urgência ou direcionamento prático imediato |
| Symptomate  | Interface simples e questionário guiado para avaliação de sintomas | Pode apresentar múltiplos resultados sem priorização clara do nível de urgência                     |

---

### Diferencial do Projeto:

Apesar da existência de diversas plataformas de verificação de sintomas, a maioria delas possui foco em fornecer informações médicas detalhadas ou possíveis condições associadas, o que pode gerar confusão em usuários sem conhecimento técnico. 

O projeto Orienta Saúde propõe uma abordagem mais direta e objetiva, focada exclusivamente na classificação de urgência e orientação inicial, facilitando a tomada de decisão do usuário. 

---

## Público-Alvo

O sistema é voltado para:

- População em geral;
- Pessoas sem conhecimento médico;
- Usuários que buscam orientação inicial.

Perfil:

- Maior de idade;
- Uso via celular ou computador 

Contexto de uso:

- Em casa 
- Antes de decidir ir ao hospital 
- Durante dúvidas sobre sintomas 

Nível de conhecimento técnico esperado:

Conhecimento básico em navegação web. 

---

## Objetivos do Projeto

### Objetivo Geral:

Desenvolver uma plataforma web capaz de realizar triagem inicial de sintomas, classificando o nível de urgência e fornecendo orientações educativas ao usuário. 

### Objetivo Específicos:

- Implementar sistema de classificação de risco baseado em regras;
- Permitir entrada estruturada de sintomas;
- Armazenar histórico de triagens;
- Desenvolver interface intuitiva e acessível.

---

## Métricas de Sucesso (KPIs)

O sucesso do sistema será avaliado baseado nas seguintes métricas:

- Tempo de resposta inferior a 3 segundos;
- Precisão das regras de triagem;
- Cobertura de sintomas implementados: mínimo de 15 sintomas diferentes.

Latência inferior a 250ms