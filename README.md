# Local Docker stack

Local Keycloak 26 with PostgreSQL 16, Redis 8, Kafka 4, Jenkins 2 LTS, Open WebUI 0, Elasticsearch 9 with Kibana 9, pgAdmin 4 9, GitLab CE 19, Nextcloud 35, MinIO-compatible Silo 2026, and Homer 26. Docker Compose starts the stack. Every published port listens on `127.0.0.1`, so only this machine can open them.

This stack is for local use. Keycloak runs `start-dev` and serves HTTP. Kafka is a single KRaft broker with plaintext listeners. Elasticsearch is a single node with plaintext HTTP and security enabled. Jenkins skips the setup wizard and signs in with the admin user from `.env`.

## What you need

- Docker with Compose. Docker Desktop includes both.
- About 4 GB of free memory for GitLab, plus 1 GB each for Elasticsearch and Kibana. The first GitLab start takes several minutes.
- Docker Desktop sets `vm.max_map_count` to at least 262144, which Elasticsearch requires.
- LM Studio, if you want Open WebUI to chat. Start its local server before you open a chat. The default port is `1234` (`LM_STUDIO_PORT` in `.env`).

## Install

From this directory, create `.env` from the example. Compose reads that file on every start, and it is gitignored.

PowerShell:

```powershell
Copy-Item .env.example .env
```

bash:

```bash
cp .env.example .env
```

The example values are enough to sign in on this machine. Edit `.env` before the first start if you want different passwords. Several services create their admin user only once, so a later edit leaves the existing account as it is.

If you change a password, keep these rules:

- `ELASTIC_PASSWORD` and `KIBANA_PASSWORD` need at least 6 letters or digits, with no quotes or backslashes.
- `KIBANA_ENCRYPTION_KEY` must be at least 32 characters. Kibana uses it to keep saved objects across restarts.
- `GITLAB_ROOT_PASSWORD` must be at least 8 characters. It must not contain `gitlab`, `devops`, or any part of the root name, username, or email. GitLab rejects those and then creates no administrator, so sign-in fails.
- `MINIO_ROOT_PASSWORD` must be at least 8 characters.

## Run

```bash
docker compose up -d
```

The first start pulls the images and waits on health checks. GitLab can stay in `starting` for several minutes. Check progress with:

```bash
docker compose ps
```

Open the dashboard at http://localhost:8090. It links to the services below. Cards and labels live in `homer/config.yml`, and the icons live in `homer/icons`.

## Services

| Service | Address | Sign in |
| --- | --- | --- |
| Homer | http://localhost:8090 | none |
| Keycloak | http://localhost:8080 | `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` |
| Jenkins | http://localhost:8081 | `JENKINS_ADMIN_ID` / `JENKINS_ADMIN_PASSWORD` |
| Open WebUI | http://localhost:3000 | the first account you create |
| Kibana | http://localhost:5601 | `elastic` / `ELASTIC_PASSWORD` |
| Elasticsearch | http://localhost:9200 | `elastic` / `ELASTIC_PASSWORD` |
| pgAdmin | http://localhost:5050 | `PGADMIN_DEFAULT_EMAIL` / `PGADMIN_DEFAULT_PASSWORD` |
| GitLab | http://localhost:8929 | `root` / `GITLAB_ROOT_PASSWORD` |
| Nextcloud | http://localhost:8082 | `NEXTCLOUD_ADMIN_USER` / `NEXTCLOUD_ADMIN_PASSWORD` |
| MinIO console | http://localhost:9001 | `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` |
| MinIO S3 API | http://localhost:9000 | same root user |
| Redis | `localhost:6379` | `REDIS_PASSWORD` |
| Kafka | `localhost:9092` | none |

Names in backticks are keys in `.env`.

Postgres has no published port. pgAdmin reaches it on the Compose network as `postgres:5432` and registers that server on each start from `POSTGRES_USER` and `POSTGRES_PASSWORD`, so a later database password edit shows up the next time pgAdmin starts.

Kafka clients on this machine use `localhost:9092`. Containers on this Compose network use `kafka:19092`.

GitLab SSH clones use `ssh://git@localhost:2224`. Jenkins agents can connect on `localhost:50000` after you enable the agent port in Jenkins. Jenkins and GitLab leave the Docker socket unmounted.

## What happens on start

`elasticsearch-setup` sets the `kibana_system` password from `KIBANA_PASSWORD` each time it runs, then exits. Kibana signs in to Elasticsearch with that user. The `elastic` user password is applied only on the first start.

`nextcloud-db-setup` creates the Nextcloud database and user from `NEXTCLOUD_DB`, `NEXTCLOUD_DB_USER`, and `NEXTCLOUD_DB_PASSWORD`, then exits. It resets that database password each time it runs. Nextcloud uses that database on the shared Postgres server, and it uses Redis for file locking. Files stay in the `nextcloud_data` volume.

Open WebUI calls LM Studio at `http://host.docker.internal:1234/v1`. `host.docker.internal` is mapped with `extra_hosts`, because `127.0.0.1` inside the container is the container itself. `WEBUI_SECRET_KEY` signs sessions; changing it logs everyone out. Chats stay in the `open_webui_data` volume. If an account already exists, confirm the connection under Admin Settings, Connections.

MinIO runs `pgsty/silo`, a MinIO-compatible build. The `minio/minio` image is gone from Docker Hub, and `quay.io/minio/minio` requires a login. The console, S3 API, and root user settings are the same. Buckets stay in the `minio_data` volume.

These accounts are stored on first start and stay in their volumes: Keycloak admin, Jenkins admin, `elastic`, GitLab `root`, Nextcloud admin, and MinIO root. Editing `.env` later leaves them unchanged. `KIBANA_PASSWORD` and `NEXTCLOUD_DB_PASSWORD` are applied again each time their setup containers run.

## Restart

Restart the whole stack:

```bash
docker compose restart
```

Restart one service by its Compose name. The others keep running:

```bash
docker compose restart keycloak
```

Names: `postgres`, `keycloak`, `redis`, `kafka`, `jenkins`, `open-webui`, `elasticsearch`, `elasticsearch-setup`, `kibana`, `pgadmin`, `nextcloud-db-setup`, `nextcloud`, `gitlab`, `minio`, `homer`.

`elasticsearch-setup` and `nextcloud-db-setup` exit after they finish. Restarting either one runs that setup again.

`docker compose restart` keeps the current containers. After you edit `.env` or `docker-compose.yml`, apply the change with `docker compose up -d`, or with `docker compose up -d keycloak` for one service.

## Stop

```bash
docker compose down
```

`docker compose down -v` also deletes the PostgreSQL, Redis, Kafka, Jenkins, Open WebUI, Elasticsearch, Kibana, pgAdmin, Nextcloud, GitLab, and MinIO volumes. That removes every realm, Redis value, Kafka topic, Jenkins home, Open WebUI chat and account, Elasticsearch index, Kibana saved object, pgAdmin setting, Nextcloud file and account, GitLab project, and MinIO bucket.
