# 🚀 BetAI - Inteligência Artificial para Análise de Partidas

Bem-vindo ao **BetAI**, uma aplicação robusta e moderna desenvolvida em **Spring Boot** que utiliza o poder da **Inteligência Artificial (Google Gemini)** para fornecer análises profundas e automatizadas de partidas de futebol.

---

## 🛠️ Tecnologias Utilizadas

Este projeto foi construído com o que há de mais moderno no ecossistema Java e AWS:

- **Linguagem:** Java 25
- **Framework:** Spring Boot 3.5.10
- **Cloud (AWS):**
  - **DynamoDB:** Persistência de dados escalável e de baixa latência para partidas, estatísticas e resultados da IA.
  - **SQS (Simple Queue Service):** Fila `match-analyser-betai` para processamento assíncrono e resiliente das análises.
  - **Parameter Store (SSM):** Gerenciamento seguro de configurações e segredos.
  - **ECR:** Registro de imagens Docker para deploy.
- **AI:** Google Gemini 2.0 Flash Lite para análise preditiva multicritério (Search + Stats).
- **Notificações:** Integração com Telegram (Bot API) e simulação de WhatsApp.
- **Infraestrutura:** Docker e GitHub Actions (CI/CD).

---

## 🏗️ Arquitetura e Fluxo de Processamento

O BetAI opera em um fluxo de processamento orientado a eventos e agendamentos:

1.  **Coleta e Filtro:** O sistema monitora o DynamoDB em busca de jogos do dia.
2.  **Mensageria:** Jogos aptos (com estatísticas e sem análise prévia) são enviados para a fila **AWS SQS**.
3.  **Consumo Assíncrono:** Um worker consome a fila (limite de 1 por vez para gerenciar rate limits da AI).
4.  **Análise de IA:** O Gemini processa o contexto (fixture + stats + predictions) e gera um JSON estruturado.
5.  **Persistência:** O resultado é salvo no campo `iaAnalysis` do registro da partida no DynamoDB.
6.  **Notificação:** Se a análise indicar valor (EV+), uma mensagem formatada é enviada ao canal do Telegram.

---

## ⏲️ Agendamentos (Schedulers)

A aplicação possui mecanismos automáticos de verificação:

*   **Verificação de Jogos Próximos:** Executa a cada **30 minutos**. Filtra partidas que começarão nas próximas **2 horas** e as envia para a fila de análise.
*   **Notificação Diária:** Envio matinal do resumo de todas as partidas do dia para os canais de comunicação.

---

## ⚖️ Regras de Negócio e Funcionalidades

*   **Cálculo de EV (Expected Value):** O sistema recalcula o valor esperado no backend usando a probabilidade da IA vs Odd da casa. Apenas análises com **EV Positivo** são notificadas.
*   **Reparo de JSON Dinâmico:** Implementação de algoritmo para corrigir respostas truncadas da IA, fechando chaves/colchetes e limpando vírgulas pendentes, garantindo alta taxa de sucesso no processamento.
*   **Identificação de Vencedor:** Lógica inteligente para mapear o mercado sugerido pela IA (ex: "Vitória Mandante", "1X", "Handicap Home") para os IDs reais dos times no banco de dados.
*   **Resiliência SQS:** Pausa programada de 30 segundos entre mensagens para respeitar os limites de quota da API do Gemini e evitar spam no Telegram.

---

## 📡 Endpoints da API

### Partidas
- `GET /api/fixtures?date=YYYY-MM-DD`: Lista partidas por data.
- `GET /api/fixtures/{id}/analyze`: Dispara análise manual imediata.

### Filtragem e Fila
- `GET /api/fixtures/filter`: Varredura geral do dia para alimentar a fila SQS.
- `GET /api/fixtures/filter-upcoming`: Varredura de jogos das próximas 2 horas (usado pelo scheduler).

### Notificações
- `GET /api/fixtures/notify`: Dispara manual do resumo matinal.

---

## ⚙️ Configuração e Execução

### Pré-requisitos
- Docker & Docker Compose
- Contas configuradas na AWS (us-east-1)
- Chave de API do Google Gemini

### Variáveis de Ambiente (Parameter Store)
A aplicação busca as seguintes chaves no **AWS SSM**:
- `FOOTBALL_API_URL`
- `FOOTBALL_API_KEY`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_CHAT_ID`
- `GEMINI_API_KEY`

### Rodando Localmente
1. Certifique-se de que suas credenciais AWS estão configuradas localmente (`~/.aws/credentials`).
2. Execute o build:
   ```bash
   ./mvnw clean package
   ```
3. Suba com Docker:
   ```bash
   docker-compose up --build
   ```

---

## 🚢 CI/CD (GitHub Actions)

O deploy é automatizado via GitHub Actions. Ao realizar um push para a branch `main`:
1. O código é compilado com **JDK 25**.
2. Uma nova imagem Docker é construída.
3. A imagem é enviada para o **Amazon ECR** (`workspace/betai`).

Certifique-se de configurar os seguintes Secrets no seu repositório GitHub:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

---

## 📄 Licença

Este projeto é de uso privado e confidencial.

---
*Desenvolvido com ❤️ pelo time BetAI.*
