# 🩺 Orienta Saúde

Plataforma web de **triagem inicial de sintomas de saúde**, que conduz uma entrevista adaptativa com o usuário e, utilizando **IA generativa (Gemini API)** e **RAG (Retrieval-Augmented Generation)**, gera orientações educativas e uma estimativa de nível de urgência (Leve, Moderado, Urgente ou Emergência).

> ⚠️ O Orienta Saúde **não realiza diagnóstico médico** e **não substitui avaliação médica presencial**. As orientações geradas têm caráter exclusivamente educativo.

Projeto desenvolvido como Trabalho de Conclusão de Curso (TCC) — Universidade Católica de Santa Catarina, 7º/8º semestre.

---

## 📋 Índice

- [Sobre o projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Stack tecnológica](#-stack-tecnológica)
- [Arquitetura](#-arquitetura)
- [Pré-requisitos](#-pré-requisitos)
- [Instalação e execução](#-instalação-e-execução)
- [Variáveis de ambiente](#-variáveis-de-ambiente)
- [Estrutura do repositório](#-estrutura-do-repositório)
- [Testes](#-testes)
- [Documentação completa (RFC)](#-documentação-completa-rfc)
- [Roadmap](#-roadmap)
- [Autor](#-autor)

---

## 📖 Sobre o projeto

O acesso inicial a orientação em saúde ainda é um desafio: muitas pessoas recorrem à internet ao sentir sintomas e ficam confusas com informações genéricas, sem saber se devem procurar atendimento imediato ou aguardar.

O **Orienta Saúde** propõe uma abordagem direta e objetiva: em vez de listar dezenas de condições possíveis, o foco é **classificar o nível de urgência** e fornecer orientação educativa clara, ajudando o usuário a decidir o próximo passo.

## ✨ Funcionalidades

- 🔐 Cadastro e login de usuários (autenticação via JWT)
- 💬 Entrevista adaptativa: perguntas geradas dinamicamente pela IA com base nas respostas anteriores
- 📚 Recuperação de conhecimento médico via RAG (base de fontes públicas: Ministério da Saúde, OMS, Fiocruz)
- 🚦 Classificação de urgência em 4 níveis: **Leve · Moderado · Urgente · Emergência**
- 📝 Geração de orientações educativas, sinais de alerta e recomendações — sem diagnóstico ou prescrição
- 🕘 Histórico de triagens anteriores por usuário
- 📱 Interface responsiva e acessível

## 🛠 Stack tecnológica

| Camada        | Tecnologia                          |
|---------------|--------------------------------------|
| Backend       | Java + Spring Boot                  |
| Frontend      | React + Vite + TypeScript           |
| Banco de dados| MySQL                               |
| IA Generativa | Gemini API                          |
| RAG           | Base de conhecimento indexada (documentos de saúde) |

## 🏗 Arquitetura

O sistema segue arquitetura em camadas no backend (Controllers → Services → Repositories), com um módulo dedicado de RAG para recuperação contextual de conhecimento antes da chamada ao modelo de IA.

Diagramas C4, DER e de sequência completos estão disponíveis no [RFC](./documents/RFC.md).

## ✅ Pré-requisitos

- [Java 21+](https://adoptium.net/)
- [Node.js 18+](https://nodejs.org/) e npm/yarn
- [MySQL 8.0+](https://dev.mysql.com/downloads/)
- Uma chave de API do [Gemini](https://ai.google.dev/gemini-api/docs)
- Maven

## 🚀 Instalação e execução

### 1. Clonar o repositório

```bash
git clone https://github.com/danisantosss/Orienta-Saude.git
cd Orienta-Saude
```

### 2. Configurar o banco de dados

```sql
CREATE DATABASE orienta_saude;
```

### 3. Backend (Spring Boot)

```bash
cd backend
# configure as variáveis de ambiente (veja seção abaixo)
./mvnw spring-boot:run
```

O backend sobe por padrão em `http://localhost:8080`

### 4. Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

O frontend sobe por padrão em `http://localhost:5173`

## 🔑 Variáveis de ambiente

Crie um arquivo `.env` (frontend) e configure `application.properties` / `application.yml` (backend) com:

```bash
# Backend
DB_URL=jdbc:mysql://localhost:3306/orienta_saude
DB_USERNAME=root
DB_PASSWORD=
JWT_SECRET=
GEMINI_API_KEY=

# Frontend
VITE_API_BASE_URL=http://localhost:8080
```

## 📂 Estrutura do repositório

```
Orienta-Saude/
├── backend/          # API REST (Java + Spring Boot)
├── frontend/          # Aplicação web (React + Vite + TypeScript)
├── images/            # Diagramas, mockups e gráficos usados na documentação
├── documents/         # RFC e documento de requisitos do TCC
└── README.md
```

## 🧪 Testes

```bash
# Backend
cd backend
./mvnw test

# Frontend
cd frontend
npm run test
```

Metas de cobertura definidas no projeto: mínimo de 75% no backend e 25% no frontend (line coverage).

## 📄 Documentação completa (RFC)

O documento RFC completo — contexto, personas, requisitos funcionais e não funcionais, mockups, arquitetura C4 e cronograma — está disponível em [`documents/RFC.md`](./documents/RFC.md).

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
Projeto de TCC — Universidade Católica de Santa Catarina

---

<sub>Este projeto é exclusivamente educativo e não substitui avaliação médica profissional.</sub>