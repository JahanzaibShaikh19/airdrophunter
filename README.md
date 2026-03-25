# 🎯 AirdropHunter

AirdropHunter is an enterprise-grade, real-time DeFi intelligence platform designed to track, analyze, and alert users about the most lucrative cryptocurrency airdrops.

Built as a modern monorepo, it features a highly concurrent **Kotlin / Spring Boot 3** backend and a premium, glassmorphism-styled **Next.js 14** frontend, backed by robust CI/CD pipelines, Telegram bot integrations, and Gumroad payment webhooks for PRO subscriptions.

---

## ⚡ Key Features

- **Real-Time Market Intelligence**: Aggregates airdrop data via DefiLlama APIs, tracking deadlines, estimated values, and step-by-step participation criteria.
- **Wallet Checker**: Instantly scan EVM addresses to discover historical on-chain eligibility across tracked projects.
- **PRO Membership (Gumroad)**: Seamless JWT-based authentication system unlocking uncapped dashboard features and API access for premium users.
- **Native Telegram Bot (`@AirdropHunterBot`)**: Automated broadcast notification system alerting PRO subscribers instantly when *HOT* or *NEW* airdrops drop, plus 24-hour expiration warnings.
- **Aesthetic Excellence**: Frontend built with Next.js 14, Tailwind CSS, SWR, and Framer Motion for a stunning, responsive, and real-time dashboard experience.
- **Zero-Downtime CI/CD**: Fully automated GitHub Actions deployment pipeline gating releases to Vercel (Frontend) and Railway (Backend) via comprehensive test suites.

---

## 🏗️ Architecture Stack

### Backend (`/backend`)
- **Language**: Kotlin 1.9 (Coroutines enabled)
- **Framework**: Spring Boot 3.2.5
- **Database**: PostgreSQL 15 + Flyway Migrations (`spring-boot-starter-data-jpa`)
- **Security**: Spring Security + JJWT (Stateless Authentication)
- **Integrations**: `telegrambots-spring-boot-starter` (Long-polling Bot)

### Frontend (`/frontend`)
- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS + Shadcn UI + Framer Motion (Glassmorphism aesthetics)
- **Data Fetching**: SWR (Auto-refresh intervals)

### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Orchestration**: GitHub Actions (`deploy.yml`)
- **Deployment Targets**: Vercel (Next.js) & Railway (Spring Boot)

---

## 🚀 Getting Started (Local Development)

### Prerequisites
- JDK 21
- Node.js 18+
- Docker & Docker Compose
- A Telegram Bot Token (acquired from [@BotFather](https://t.me/BotFather))

### 1. Environment Setup
Copy the example environment mappings in the root directory:
```bash
cp .env.example .env
```
Ensure your `.env` is populated correctly. Essential keys include `JWT_SECRET`, `TELEGRAM_BOT_USERNAME`, and `TELEGRAM_BOT_TOKEN`. (See `DEPLOYMENT.md` for full parameter documentation).

### 2. Run Infrastructure (PostgreSQL)
Start the foundational database using Docker Compose:
```bash
docker-compose up postgres -d
```

### 3. Start the Backend (Kotlin / Spring Boot)
The backend uses Flyway, so database schemas (`V1` through `V5`) will automatically generate and seed dummy data on startup.
```bash
cd backend
./gradlew bootRun
```
*The backend will be available at `http://localhost:8080/api`.*

### 4. Start the Frontend (Next.js)
```bash
cd frontend
npm install --legacy-peer-deps
npm run dev
```
*The dashboard will be available at `http://localhost:3000`.*

---

## 🧪 Testing

The backend contains a rigorous, multi-layered testing suite validating both REST slices and unit isolation:
```bash
cd backend
./gradlew test
```
**Coverage Includes:**
- `GumroadWebhookControllerTest`: Validates HMAC-SHA256 signature verifications.
- `ProUserServiceTest`: Validates expiration logic and license state toggles.
- `AirdropHunterBotTest`: Validates Telegram command parsings (`/start`, `/link`, `/alerts`).
- `JwtServiceTest`: Validates token issuer encryptions.

---

## 📦 Deployment via GitHub Actions

This repository is configured to deploy automatically on pushes to the `main` branch. 
The pipeline (`.github/workflows/deploy.yml`) ensures safety by running the entire JVM test suite prior to issuing deployment commands.

1. Committing to `main` triggers the Action.
2. If Tests Pass -> Deploys Backend container to **Railway** natively via `railway.json` and Docker build.
3. If Backend Deploys -> Deploys Frontend to **Vercel** with Next.js rewrites securely proxying API layers to the Railway domain to circumvent CORS.

See [`DEPLOYMENT.md`](./DEPLOYMENT.md) for configuring the expected GitHub Secrets (`VERCEL_TOKEN`, `RAILWAY_TOKEN`, etc.).

---

## 📄 License & Legal
AirdropHunter is proprietary software. All rights reserved. 
*(Adjust license terms here according to organizational policy).*