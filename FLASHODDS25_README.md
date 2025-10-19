
# FlashOdds

A Java 25 + Spring Boot + React 19 app that shows free sports odds with a flashy **AG Grid** UI.  
Now includes **live updates** via WebSocket with SSE fallback, multi‑provider enrichment, and a single‑jar deployment.

---

## Highlights

- **Java 25** with virtual threads, records, pattern matching `switch`, sequenced collections
- **Spring Boot 3.3+** with WebFlux, Actuator, CORS, Rate limiting, Caching
- **Live updates**: WebSocket `/ws/odds` primary, Server Sent Events `/api/odds/stream` fallback
- **Providers**: 
  - Odds: The Odds API free tier (optional key) or mock data
  - Enrichment (optional): `balldontlie` NBA stats, NHL Stats API, MLB Stats API, team logos via static mapping
- **React 19 + Vite + TypeScript** with **AG Grid Community**
- **Single jar** serves built SPA, or run split dev servers with proxy
- **Flashy UI**: dark theme, animated row flashing, delta updates, toolbars, filters, quick search
- **Security first**: strict CORS in prod, API key isolation, timeouts, circuit breaker
- **Dev friendly**: hot reload, lint, format, tests, Dockerfile, Makefile targets

---

## Architecture

```
flashodds25
├─ backend (Spring Boot)
│  ├─ REST:   /api/odds         -> normalized odds
│  ├─ SSE:    /api/odds/stream  -> text/event-stream
│  ├─ WS:     /ws/odds          -> real-time frames
│  └─ Static: serves / (SPA)
└─ frontend (React 19, Vite, TS, AG Grid)
   ├─ OddsGrid with deltaRowDataMode, flashing
   ├─ Toolbar: sport, market, region, theme, refresh
   └─ Data: ws > sse > poll
```

**Live loop**  
Backend pulls provider data on a schedule using **virtual threads** and emits compact diffs to clients. Frontend applies diffs with AG Grid transactions for smooth updates.

---

## Data model

```text
OddsRow (record)
- id: String                # sport:league:eventId:book:market:runner
- sport: String             # nfl, nba, nhl, mlb
- event: String             # Team A vs Team B
- market: String            # h2h | spreads | totals
- line: Double?             # spread or total
- price: Integer            # American odds e.g. -110
- book: String              # sportsbook name
- startsAt: Instant
- updatedAt: Instant
- extra: Map<String,Object> # provider/meta
```

**WebSocket frame**

```json
{
  "type": "snapshot|delta",
  "ts": 1739900000000,
  "rows": [
    {"op":"upsert","row":{ "...": "..." }},
    {"op":"remove","id":"nfl:nos:..."}
  ]
}
```

SSE uses the same payload per event line.

---

## Providers

### Primary
- **The Odds API**: set `ODDS_PROVIDER=theoddsapi` and `ODDS_API_KEY=...`.

### Free enrichment (optional)
- **NBA**: `balldontlie` for team names and season info
- **NHL**: public NHL Stats API
- **MLB**: public MLB Stats API

> If any enrichment call fails or hits a limit, the backend continues with odds data only. All enrichers are best‑effort and cached.

---

## Prerequisites

- Java 25 on PATH
- Maven 3.9+
- Node 20+ and pnpm 9+ (`corepack enable` then `corepack prepare pnpm@latest --activate`)

---

## Setup

```bash
git clone <your-repo-url> flashodds25
cd flashodds25

# Install dependencies
pnpm install

# Frontend deps
cd frontend && pnpm install && cd ..
```

### Quick start (Makefile)

```bash
# Run backend + frontend dev servers
make dev

# Build frontend assets and package Spring Boot jar
make build

# Execute checks
make lint
make typecheck
make test
```

The `sync-frontend` task compiles the Vite bundle and syncs it into `backend/src/main/resources/static` so the Spring Boot jar serves the SPA during packaging and Docker builds.

### Run locally without Make

```bash
# Terminal 1 – Spring Boot backend on http://localhost:8080
./backend/mvnw spring-boot:run

# Terminal 2 – React app with Vite on http://localhost:5173 (proxied to backend)
pnpm --dir frontend dev
```

Visit `http://localhost:5173` for the live UI. The Vite dev server proxies `/api` and `/ws` calls back to the Spring Boot process, so hot reload works on both sides.

### Environment variables

Backend defaults live in `backend/src/main/resources/application.yml`. Override via env vars or `.env` injectors:

| Variable | Description | Default |
| --- | --- | --- |
| `ODDS_PROVIDER` | Provider key (`mock` or `theoddsapi`) | `mock` |
| `ODDS_API_KEY` | The Odds API key when `ODDS_PROVIDER=theoddsapi` | _(empty)_ |
| `ODDS_REGIONS` | Comma list of regions to request | `us` |
| `ODDS_MARKETS` | Markets to request (`h2h,spreads,totals`) | see config |
| `ODDS_REFRESH` | Poll cadence in seconds | `15` |

When using The Odds API, export `ODDS_PROVIDER=theoddsapi` and `ODDS_API_KEY=<key>` before starting the backend.

### Docker

```bash
docker build -t flashodds25 .
docker run --rm -p 8080:8080 -e ODDS_PROVIDER=mock flashodds25
```

The container exposes port `8080` serving the built SPA and API from the fat jar.

Create `backend/src/main/resources/application.yml`:

```yaml
server:
  port: 8080
spring:
  threads.virtual.enabled: true
  main.web-application-type: reactive
  codec.max-in-memory-size: 2MB

app:
  odds:
    provider: ${ODDS_PROVIDER:mock}   # mock | theoddsapi
    apiKey: ${ODDS_API_KEY:}
    regions: ${ODDS_REGIONS:us}
    markets: ${ODDS_MARKETS:h2h,spreads,totals}
    sports: ${ODDS_SPORTS:nfl,nba,nhl,mlb}
    cacheTtlSeconds: ${ODDS_CACHE_TTL:60}
    refreshSeconds: ${ODDS_REFRESH:15}     # backend pull cadence
  sse:
    enabled: true
  ws:
    enabled: true

management:
  endpoints.web.exposure.include: "health,info,metrics"
```

Environment variables for live data:
```bash
export ODDS_PROVIDER=theoddsapi
export ODDS_API_KEY=YOUR_KEY
# optional
export ODDS_REGIONS=us
export ODDS_MARKETS=h2h,spreads,totals
export ODDS_SPORTS=nfl,nba,nhl,mlb
```

---

## Run

### Dev split mode with proxy
```bash
# terminal 1
cd backend
mvn spring-boot:run

# terminal 2
cd ../frontend
pnpm dev
# open http://localhost:5173
```

### Single jar serving SPA
```bash
pnpm --dir frontend build
mvn -q -T 1C -DskipTests package
java -jar backend/target/backend-*.jar
# open http://localhost:8080
```

---

## Live updates: details

### WebSocket
- Endpoint: `/ws/odds`
- Protocol: text JSON frames
- Broadcast model: backend keeps a compact in‑memory state and computes diffs each refresh; sends `delta` frames only
- Backpressure: bounded queue per session, oldest drop on overflow
- Heartbeats: server pings every 20 seconds

Frontend snippet:
```ts
const url = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/odds`;
const ws = new WebSocket(url);
ws.onmessage = (ev) => {
  const msg = JSON.parse(ev.data);
  if (msg.type === 'snapshot') gridApi.setRowData(msg.rows.map(r => r.row));
  if (msg.type === 'delta') gridApi.applyTransactionAsync({
    add: msg.rows.filter(x => x.op === 'upsert').map(x => x.row),
    update: msg.rows.filter(x => x.op === 'upsert').map(x => x.row),
    remove: msg.rows.filter(x => x.op === 'remove').map(x => ({ id: x.id })),
  });
};
```

### SSE fallback
- Endpoint: `/api/odds/stream` with `text/event-stream`
- Reconnect on close with exponential backoff
- Same payload as WS frames

Frontend snippet:
```ts
const es = new EventSource('/api/odds/stream');
es.onmessage = (e) => { /* same handling as WS */ };
```

### Polling fallback
- `setInterval(fetchOdds, 15000)` used only if both WS and SSE fail.

---

## Frontend: AG Grid features

- `deltaRowDataMode` with `getRowId`
- Row flashing on updates
- Value formatters for American odds and spreads
- Quick filter and column tool panels
- Pinned columns for teams and market
- Dark theme plus custom theme toggle
- Responsive layout with toolbar and stats chips

---

## Performance

- Virtual threads handle I O bound provider calls efficiently
- Caching layer with TTL for provider responses
- Minimal allocations in diff calculation
- GZIP static assets and JSON responses
- HTTP timeouts 2s connect, 5s read by default

---

## Security

- Do not log API keys
- In prod, **disable CORS** except for your domain
- Use **Full Strict TLS** behind your proxy
- Rate limit: simple token bucket at controller level
- Circuit breaker for provider outages

---

## Testing

- **Backend**: JUnit 5 + WebFlux test; contract tests for payload shape
- **Frontend**: Vitest + React Testing Library; snapshot for grid columns; WS event handler tests with mocked frames

```bash
mvn -q test
pnpm --dir frontend test
```

---

## Tooling

- **Makefile**
  - `make dev`  run both apps
  - `make build` package jar and SPA
  - `make test`  run all tests
- **Docker**
  - `docker build -t flashodds25 .`
  - `docker run -p 8080:8080 -e ODDS_PROVIDER=mock flashodds25`

---

## CI example (GitHub Actions)

- JDK 25 setup
- pnpm cache
- Maven package with frontend step
- Artifact upload of the fat jar

---

## Troubleshooting

- 401 from odds: check `ODDS_API_KEY`
- No live frames: verify WS URL, reverse proxy `Connection: Upgrade` and `Upgrade: websocket`
- CORS in dev: Vite proxy should target `http://localhost:8080`
- Blank SPA on refresh: ensure unknown paths route to `/index.html`

---

## Roadmap

- Additional markets and books
- Historical odds timeline
- Favorites and alerts
- Persist snapshots to Postgres with R2DBC
- Admin page for provider health
- Theming presets and compact grid density

---

## License

MIT for the scaffold. Check third‑party API terms.
