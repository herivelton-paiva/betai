# 🚀 BetAI - Inteligência Artificial para Análise de Partidas

Bem-vindo ao **BetAI**, uma aplicação robusta e moderna desenvolvida em **Spring Boot** que utiliza o poder da **Inteligência Artificial (Google Gemini)** para fornecer análises profundas e automatizadas de partidas de futebol.

---

## 🛠️ Tecnologias Utilizadas

Este projeto foi construído com o que há de mais moderno no ecossistema Java e AWS:

- **Linguagem:** Java 25
- **Framework:** Spring Boot 3.5.10
- **Cloud (AWS):**
  - **DynamoDB:** Persistência de dados escalável e de baixa latência.
  - **Parameter Store (SSM):** Gerenciamento seguro de configurações e segredos.
  - **ECR:** Registro de imagens Docker para deploy.
- **AI:** Google Gemini API para análise preditiva e contextual.
- **Notificações:** Integração com Telegram e WhatsApp.
- **Infraestrutura:** Docker e GitHub Actions (CI/CD).

---

## 📡 Endpoints da API

### Partidas
- `GET /api/fixtures?date=YYYY-MM-DD`: Retorna as partidas da data informada.
- `GET /api/fixtures/{id}/analyze`: Realiza a análise de IA para uma partida específica.

### Notificações
- `GET /api/fixtures/notify`: Dispara manualmente o envio de notificações do dia.

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
