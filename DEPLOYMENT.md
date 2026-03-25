# Deployment Environment Checklists

Before initializing the `Github Actions` CI/CD pipeline, ensure all secret parameters are configured accurately per deployment environment.

## 1. Local Development (`.env` or Docker configuration)
- [ ] `DATABASE_URL` (e.g., `jdbc:postgresql://localhost:5432/airdrops`)
- [ ] `SPRING_DATASOURCE_USERNAME` / `PASSWORD`
- [ ] `JWT_SECRET` (minimum 32 character alphanumeric base64)
- [ ] `TELEGRAM_BOT_USERNAME` & `TELEGRAM_BOT_TOKEN`
- [ ] `GUMROAD_WEBHOOK_SECRET` (Can be empty array for local debug bypassing)

## 2. GitHub Action Repository Secrets (`Settings > Secrets and variables > Actions`)
- [ ] `RAILWAY_TOKEN` (Generated from Railway Project Settings)
- [ ] `VERCEL_TOKEN` (Generated from Vercel Account Settings > Tokens)
- [ ] `VERCEL_ORG_ID` (Found in Vercel scope lookup)
- [ ] `VERCEL_PROJECT_ID` (Generated upon linking the Vercel project)

## 3. Server Variables (Railway Dashboard)
Navigate to **Railway Platform** -> **Variables** for the Kotlin Service:
- [ ] `DATABASE_URL` (Usually linked natively via Railway Postgres add-on)
- [ ] `REDIS_URL` (If utilizing cross-pod caching)
- [ ] `JWT_SECRET` (Must be cryptographically secure in Prod!)
- [ ] `JWT_EXPIRATION_MS` = `604800000` (7 Days)
- [ ] `TELEGRAM_BOT_USERNAME` & `TELEGRAM_BOT_TOKEN`
- [ ] `GUMROAD_WEBHOOK_SECRET` (Generate this from your Gumroad Product Settings > Ping)
- [ ] *[Future Architecture]* `ETHERSCAN_API_KEY` (Required when replacing the dummy hash wallet checker)
- [ ] *[Future Architecture]* `ALCHEMY_API_KEY` (Web3 RPC node resolution)

## 4. Frontend Variables (Vercel Settings)
Navigate to **Vercel** -> **Project Settings** -> **Environment Variables**:
- [ ] `NEXT_PUBLIC_API_URL` -> `https://backend.up.railway.app/api` (Ensure this URL correctly matches your generated Railway domain).
