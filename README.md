# Keycloak

Local Keycloak 26.8.1 with PostgreSQL 16, Redis 8, Kafka 4.4, Jenkins 2.580.2 LTS, Open WebUI 0.12.5, Elasticsearch 9.5.6 with Kibana 9.5.6, pgAdmin 4 9.19, GitLab CE 19.5.3, Nextcloud 35.0.2, MinIO-compatible Silo RELEASE.2026-10-26, and Homer 26.08.3, started with Docker Compose. Published ports listen on `127.0.0.1` only.

`start-dev` is for local use. It serves HTTP and is not a production configuration. Kafka is a single KRaft broker with plaintext listeners, also for local use. Elasticsearch is a single node with plaintext HTTP and security enabled, also for local use. Jenkins skips the setup wizard and signs in with the admin user from `.env`.

## Start

```bash
docker compose up -d
```

Admin console: http://localhost:8080

Default credentials are in `.env` (`admin` / `admin`). Change them before using this anywhere other than your machine. The bootstrap admin is created only on the first start; later edits to `KEYCLOAK_ADMIN_PASSWORD` do not change an existing user.

Redis listens on `localhost:6379`. The password is `REDIS_PASSWORD` in `.env`.

Kafka listens on `localhost:9092` for clients on the host. Containers on this Compose network should use `kafka:19092`.

Jenkins is at http://localhost:8081. Default credentials are in `.env` (`admin` / `admin`). The admin user is created only on the first start; later edits to `JENKINS_ADMIN_PASSWORD` do not change an existing user. Inbound agents can connect on `localhost:50000` after the agent port is enabled in Jenkins. The Docker socket is not mounted.

Open WebUI is at http://localhost:3000. The first account you create is the admin. `WEBUI_SECRET_KEY` in `.env` signs sessions; changing it logs everyone out. Chats and settings stay in the `open_webui_data` volume. It uses the LM Studio server on the host at `http://host.docker.internal:1234/v1` (`LM_STUDIO_PORT` and `LM_STUDIO_API_KEY` in `.env`). `host.docker.internal` is mapped with `extra_hosts`, because `127.0.0.1` inside the container is the container itself. Start LM Studio's local server before chatting. If you already created an Open WebUI account, confirm the connection under Admin Settings, Connections.

Homer is at http://localhost:8090. It lists the services on this page. Cards and labels live in `homer/config.yml`, and the icons live in `homer/icons`.

Elasticsearch listens on `localhost:9200`. Sign in as `elastic` with `ELASTIC_PASSWORD` from `.env`. That password is applied only on the first start; later edits do not change an existing cluster. Kibana is at http://localhost:5601 and uses the same `elastic` login. It connects to Elasticsearch as `kibana_system`. The `elasticsearch-setup` service sets that password from `KIBANA_PASSWORD` each time it runs, then exits. Both passwords need at least 6 letters or digits, with no quotes or backslashes. `KIBANA_ENCRYPTION_KEY` must be at least 32 characters so saved objects survive a restart. Elasticsearch and Kibana each have a 1 GB memory limit. Indexes stay in the `elasticsearch_data` volume, and Kibana state stays in `kibana_data`. Docker Desktop needs `vm.max_map_count` of at least 262144; current Desktop releases already set that.

pgAdmin is at http://localhost:5050. Sign in with `PGADMIN_DEFAULT_EMAIL` and `PGADMIN_DEFAULT_PASSWORD` from `.env` (`admin@example.com` / `admin`). That account is created only on the first start. The Postgres server is registered on each start from `POSTGRES_USER` and `POSTGRES_PASSWORD`, so a later password edit is picked up the next time pgAdmin starts. pgAdmin reaches Postgres on the Compose network; Postgres itself is not published on the host.

GitLab is at http://localhost:8929. Sign in as `root` with `GITLAB_ROOT_PASSWORD` from `.env`. The password must be at least 8 characters and must not contain `gitlab`, `devops`, or any part of the root name, username, or email (`admin` from `admin@example.com`). GitLab rejects those and then creates no administrator, so sign-in fails. The password is applied only on the first start. The first start takes several minutes and wants about 4 GB of free memory. Puma runs as a single process and the bundled Prometheus is off, which keeps a local instance smaller. Clone over SSH at `ssh://git@localhost:2224`. The Docker socket is not mounted.

Nextcloud is at http://localhost:8082. Sign in with `NEXTCLOUD_ADMIN_USER` and `NEXTCLOUD_ADMIN_PASSWORD` from `.env` (`admin` / `admin`). That account is created only on the first start. It uses its own database on the shared Postgres server. `nextcloud-db-setup` creates that database and user from `NEXTCLOUD_DB`, `NEXTCLOUD_DB_USER`, and `NEXTCLOUD_DB_PASSWORD`, then exits. It also resets the database password each time it runs. File locking uses the existing Redis. Files and the Nextcloud install stay in the `nextcloud_data` volume.

MinIO's console is at http://localhost:9001. The S3 API listens on `localhost:9000`. Sign in with `MINIO_ROOT_USER` and `MINIO_ROOT_PASSWORD` from `.env` (`minioadmin` / `minioadmin`). The root password must be at least 8 characters and is applied only on the first start. Buckets stay in the `minio_data` volume. The service runs `pgsty/silo`, a MinIO-compatible build, because MinIO removed `minio/minio` from Docker Hub and `quay.io/minio/minio` now requires a login. The console, S3 API, and `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` settings are unchanged.

## Stop

```bash
docker compose down
```

`docker compose down -v` also deletes the PostgreSQL, Redis, Kafka, Jenkins, Open WebUI, Elasticsearch, Kibana, pgAdmin, Nextcloud, GitLab, and MinIO volumes. That removes every realm, Redis value, Kafka topic, Jenkins home, Open WebUI chats and accounts, Elasticsearch index, Kibana saved object, pgAdmin setting, Nextcloud file and account, GitLab project, and MinIO bucket.
