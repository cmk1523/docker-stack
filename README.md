# Local Docker stack

Local Keycloak 26 with PostgreSQL 16, Redis 8, Kafka 4, Jenkins 2 LTS, Open WebUI 0, Open WebUI Computer 0.9, Elasticsearch 9 with Kibana 9, pgAdmin 4 9, GitLab CE 19, Nextcloud 35, MinIO-compatible Silo 2026, and Homer 26. Docker Compose starts the stack. Every published port listens on `127.0.0.1`, so only this machine can open them.

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

Create the external Docker network that the `computer` service joins. Compose declares it as `external`, so it will not create it for you, and `docker compose up` fails without it:

```bash
docker network create keycloak-container_default
```

The command only needs to run once per Docker install. If the network already exists, it prints an error you can ignore.

## Run

```bash
docker compose up -d
```

The first start pulls the images and waits on health checks. GitLab can stay in `starting` for several minutes. Check progress with:

```bash
docker compose ps
```

Open the dashboard at http://localhost:8090. It links to the services below. Cards and labels live in `homer/config.yml`, and the icons live in `homer/icons`.

## Troubleshooting

**`network keycloak-container_default declared as external, but could not be found`**
Create the network with `docker network create keycloak-container_default`, then run `docker compose up -d` again. Nothing else is wrong. Compose checks this before it starts any container.

**`The "POSTGRES_USER" variable is not set. Defaulting to a blank string.`** (and similar lines for other variables)
`.env` is missing, or you are running Compose from a different directory. Run `cp .env.example .env` and run Compose from this directory. Do not start the stack with blank variables. Services such as Postgres and Elasticsearch store those blank values on first start. If that already happened, `docker compose down -v` clears it. That deletes all data, so only do it on a fresh stack.

**`permission denied while trying to connect to the docker API`**
Docker Desktop is not running, or the terminal cannot reach its socket. Start Docker Desktop and try again.

**Other errors on `docker compose up -d`**
Check which services are unhealthy with `docker compose ps -a`, then read that service's log with `docker compose logs <service>`.

**A port is already in use**
Every published port is bound to `127.0.0.1` and is fixed in `docker-compose.yml`. Stop whatever else uses it, or change the left side of that `ports` entry. For example, `127.0.0.1:8080:8080` becomes `127.0.0.1:18080:8080`.

**GitLab is slow or returns 502**
This is normal for the first minute or two, and the first start can take longer. Wait until `docker compose ps gitlab` shows `healthy`. Opening `http://localhost:8929/-/liveness` from your browser returns 404 even when GitLab is fine. The health check runs inside the container, where it works.

**What a working stack looks like**
These responses are expected and are not errors:

- Elasticsearch at http://localhost:9200 returns `401` without credentials. Use `elastic` / `ELASTIC_PASSWORD`.
- Keycloak at http://localhost:8080 redirects (`302`) to its admin console.
- `elasticsearch-setup` and `nextcloud-db-setup` show `Exited (0)`. They run once and stop.
- Kibana waits for `elasticsearch-setup`, so it starts a little after Elasticsearch.

## Services

| Service | Address | Sign in |
| --- | --- | --- |
| Homer | http://localhost:8090 | none |
| Keycloak | http://localhost:8080 | `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` |
| Jenkins | http://localhost:8081 | `JENKINS_ADMIN_ID` / `JENKINS_ADMIN_PASSWORD` |
| Open WebUI | http://localhost:3000 | the first account you create |
| Computer | http://localhost:8000 | the account from the first-start setup link |
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

Computer keeps its account, settings, and chats in the `computer_data` volume. The first start prints a one-time setup link in the logs (`docker compose logs computer`); open that link to create the account. After an account exists, sign in at http://localhost:8000. Files live in `computer/workspace` on this machine, which the container sees as `/workspace`. Add `/workspace` as a workspace in the app. A signed-in user can use the shell and files inside that folder. The same `host.docker.internal` name reaches LM Studio if you point a model at it. The image tag `0.9.21-browser` adds Chromium when an agent needs a browser; this stack uses the default image.

MinIO runs `pgsty/silo`, a MinIO-compatible build. The `minio/minio` image is gone from Docker Hub, and `quay.io/minio/minio` requires a login. The console, S3 API, and root user settings are the same. Buckets stay in the `minio_data` volume.

These accounts are stored on first start and stay in their volumes: Keycloak admin, Jenkins admin, `elastic`, GitLab `root`, Nextcloud admin, MinIO root, and the Computer account. Editing `.env` later leaves them unchanged. `KIBANA_PASSWORD` and `NEXTCLOUD_DB_PASSWORD` are applied again each time their setup containers run.

## Restart

Restart the whole stack:

```bash
docker compose restart
```

Restart one service by its Compose name. The others keep running:

```bash
docker compose restart keycloak
```

Names: `postgres`, `keycloak`, `redis`, `kafka`, `jenkins`, `open-webui`, `computer`, `elasticsearch`, `elasticsearch-setup`, `kibana`, `pgadmin`, `nextcloud-db-setup`, `nextcloud`, `gitlab`, `minio`, `homer`.

`elasticsearch-setup` and `nextcloud-db-setup` exit after they finish. Restarting either one runs that setup again.

`docker compose restart` keeps the current containers. After you edit `.env` or `docker-compose.yml`, apply the change with `docker compose up -d`, or with `docker compose up -d keycloak` for one service.

## Stop

```bash
docker compose down
```

`docker compose down -v` also deletes the PostgreSQL, Redis, Kafka, Jenkins, Open WebUI, Computer, Elasticsearch, Kibana, pgAdmin, Nextcloud, GitLab, and MinIO volumes. That removes every realm, Redis value, Kafka topic, Jenkins home, Open WebUI chat and account, Computer account and chat, Elasticsearch index, Kibana saved object, pgAdmin setting, Nextcloud file and account, GitLab project, and MinIO bucket. Files in `computer/workspace` stay on this machine.
