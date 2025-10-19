# Frontend build stage
FROM node:20.17-alpine AS frontend-builder
WORKDIR /app
COPY pnpm-workspace.yaml ./
COPY pnpm-lock.yaml ./
COPY frontend/package.json frontend/
COPY frontend/tsconfig*.json frontend/
COPY frontend/vite.config.ts frontend/
COPY frontend/postcss.config.js frontend/
COPY frontend/tailwind.config.ts frontend/
COPY frontend/eslint.config.ts frontend/
RUN corepack enable && pnpm install --frozen-lockfile --filter flashodds-frontend...
COPY frontend/ frontend/
RUN pnpm --filter flashodds-frontend build

# Backend build stage
FROM eclipse-temurin:25-jdk AS backend-builder
WORKDIR /app
COPY backend/ backend/
COPY --from=frontend-builder /app/frontend/dist/ backend/src/main/resources/static/
RUN chmod +x backend/mvnw
RUN backend/mvnw -q -f backend/pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=backend-builder /app/backend/target/flashodds-backend-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
