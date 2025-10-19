# FlashOdds 25

FlashOdds 25 is a modern sports-odds viewer that pairs a reactive Spring Boot 3 backend (Java 25) with a React 19 + Vite frontend. Odds updates stream in real time over WebSockets with SSE and polling fallbacks, and the UI renders fast delta flashes in AG Grid.

## Features

- **Live odds pipeline**: WebFlux scheduler pulls provider data, diffing snapshots into WebSocket/SSE broadcasts.
- **Provider abstraction**: ships with a mock feed plus The Odds API integration (set `ODDS_PROVIDER=theoddsapi`).
- **React 19 dashboard**: filters, theme toggle, animated AG Grid updates, TanStack Query caching.
- **Tooling**: pnpm workspace, Maven wrapper, Makefile, Dockerfile, and split GitHub Actions workflows.

## Prerequisites

- Java 25 (Temurin recommended)
- Node.js 20 with pnpm (`corepack enable`)
- Maven wrapper already included (`./backend/mvnw`)

## Local Development

```bash
pnpm install                  # install workspace dependencies

# Terminal 1 – backend (http://localhost:8080)
./backend/mvnw spring-boot:run

# Terminal 2 – frontend (http://localhost:5173, proxied to backend)
pnpm --dir frontend dev
```

The Vite dev server proxies `/api` and `/ws` to the Spring Boot service, so hot reload works across the stack.

## Environment

Important vars (see `backend/src/main/resources/application.yml`):

| Variable          | Purpose                                              | Default |
|-------------------|------------------------------------------------------|---------|
| `ODDS_PROVIDER`   | `mock` or `theoddsapi`                               | `mock`  |
| `ODDS_API_KEY`    | The Odds API key when provider is `theoddsapi`       | _(none)_|
| `ODDS_REGIONS`    | Region list passed to provider                       | `us`    |
| `ODDS_MARKETS`    | Markets to fetch (comma-separated)                   | `h2h,spreads,totals` |
| `ODDS_REFRESH`    | Backend refresh cadence (seconds)                    | `15`    |

## Make Targets

```bash
make dev        # run frontend dev & Spring Boot together
make lint       # eslint over frontend
make typecheck  # tsc --noEmit
make test       # frontend vitest + backend mvnw test
make build      # build Vite bundle, sync, mvn package
```

## Docker Build

```bash
docker build -t flashodds25 .
docker run --rm -p 8080:8080 -e ODDS_PROVIDER=mock flashodds25
```

## Structure

```
backend/   Spring Boot (WebFlux, WebSocket, caching, mock provider)
frontend/  React + TypeScript + Vite + AG Grid + TanStack Query
Makefile   Convenience commands
Dockerfile Multi-stage build -> runnable Spring Boot jar
.github/   Frontend quality, backend tests, and packaging workflows
```

## Testing & Quality

- Frontend: `pnpm --dir frontend test`
- Backend: `./backend/mvnw test`
- CI: three GitHub Actions workflows run lint/typecheck/tests/build

## License

MIT for application code. Review third-party API terms (e.g., The Odds API) before production use.
