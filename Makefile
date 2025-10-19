PNPM ?= pnpm
MVNW ?= ./backend/mvnw

.PHONY: install frontend-dev backend-dev dev sync-frontend build test lint typecheck format ci docker clean

install:
	$(PNPM) install
	$(MVNW) -q dependency:go-offline

frontend-dev:
	$(PNPM) --dir frontend dev

backend-dev:
	$(MVNW) spring-boot:run

dev:
	$(PNPM) --dir frontend dev & $(MVNW) spring-boot:run

sync-frontend:
	$(PNPM) --dir frontend build
	find backend/src/main/resources/static -mindepth 1 ! -name '.gitkeep' -exec rm -rf {} +
	cp -r frontend/dist/. backend/src/main/resources/static/

build: sync-frontend
	$(MVNW) clean package

lint:
	$(PNPM) --dir frontend lint

typecheck:
	$(PNPM) --dir frontend typecheck

format:
	$(PNPM) --dir frontend format

test:
	$(PNPM) --dir frontend test
	$(MVNW) test

ci: lint typecheck test build

clean:
	rm -rf frontend/node_modules frontend/dist
	$(MVNW) clean

docker:
	docker build -t flashodds25 .
