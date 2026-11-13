# Keycloak
The `keycloak` service in `docker-compose.yml` imports `src/blog-realm.json` on startup
(`start-dev --import-realm`), so the `blog` realm is ready right after startup, no manual
configuration in the Admin Console is needed.

## Setup

1. **Hostname entry.** Keycloak announces itself as `http://keycloak:8180` (`KC_HOSTNAME_URL`), so
   the token issuer (`iss`) is the same whether the token is requested from the host or from another
   container in `blog-nw`. For the host to resolve that name, add this line to your hosts file
   (`C:\Windows\System32\drivers\etc\hosts` on Windows, `/etc/hosts` on Linux/macOS):

   ```text
   127.0.0.1 keycloak
   ```

2. **Start the container** (from the root folder):

   ```shell script
   docker-compose up -d keycloak
   ```

   The first start takes a moment until the realm is imported. The Admin Console is then available
   at <http://keycloak:8180> (or <http://localhost:8180>), login `admin` / `admin`.

The Keycloak data directory is persisted in the `keycloak_data` volume, so changes made in the
Admin Console survive a container restart. The flip side: `--import-realm` skips a realm that
already exists, so later edits to `src/blog-realm.json` are **not** picked up automatically. To
re-import the file, remove the volume first:

```shell script
docker-compose down
docker volume rm <project>_keycloak_data   # see `docker volume ls` for the exact name
docker-compose up -d keycloak
```

## Realm contents

| Item | Value |
|------|-------|
| Realm | `blog` |
| Realm roles | `reader`, `author`, `admin` (composite over `author`); `reader` is part of `default-roles-blog`, so every user gets it |
| Client | `backend-service`, confidential, secret `secret!`, *Direct Access Grants* (password grant) enabled |

Test users:

| Username | Password | Assigned role | Roles in the token (`realm_access.roles`) |
|----------|----------|---------------|---------------------------------------------|
| `alice`  | `alice`  | `admin`       | `admin`, `author`, `reader`, ...            |
| `bob`    | `bob`    | `author`      | `author`, `reader`, ...                     |

The `...` are Keycloak's built-in defaults (`offline_access`, `uma_authorization`,
`default-roles-blog`).

## Exporting the current configuration

If you changed the realm (clients, roles, users, ...) via the Admin Console and want to persist
those changes back into `src/blog-realm.json` for version control, export the realm from the
running container and copy the result out:

```shell script
docker exec -u root -it keycloak /opt/keycloak/bin/kc.sh export --dir /tmp/export --users realm_file
docker cp keycloak:/tmp/export/blog-realm.json ./src/blog-realm.json
```

The export briefly stops and restarts the embedded Keycloak server, so expect a short
unavailability while the command runs. Review the resulting diff before committing.

## Testing access tokens

Use [HTTPie](https://httpie.io/) to request an access token for one of the imported test users via
the password grant, authenticating as the `backend-service` client:

```shell script
http -v --form --auth backend-service:secret! POST http://keycloak:8180/realms/blog/protocol/openid-connect/token username=alice password=alice grant_type=password
http -v --form --auth backend-service:secret! POST http://keycloak:8180/realms/blog/protocol/openid-connect/token username=bob password=bob grant_type=password
```

Copy the value of the `access_token` field (without quotes) to your clipboard and paste it into
the debugger at [jwt.io](https://jwt.io). The decoded payload must contain the user's role under
`realm_access.roles`, e.g. for `bob`:

```json
{
  "iss": "http://keycloak:8180/realms/blog",
  "preferred_username": "bob",
  "realm_access": {
    "roles": ["author", "reader", "offline_access", "uma_authorization", "default-roles-blog"]
  }
}
```

If `http` fails with a name resolution error, the hosts entry from [Setup](#setup) is missing.

## Calling the API

These [HTTPie](https://httpie.io/) calls need the running application. Request a token for `bob` and
`alice` as shown in [Testing access tokens](#testing-access-tokens) and store each `access_token`
in a variable, repeat this step once a token has expired:

```shell script
BOB=<access_token of bob>
ALICE=<access_token of alice>
```

Each call below shows the expected status. Replace `<id>` with the `id` from the `POST` response:

```shell script
# reading is public: 200
http :8080/blogs

# writing without a token: 401
http POST :8080/blogs title=Hallo content=Welt

# author creates a blog and becomes its owner: 201
http -A bearer -a "$BOB" POST :8080/blogs title=Hallo content=Welt

# author edits their own blog: 200
http -A bearer -a "$BOB" PATCH :8080/blogs/<id> content='Neuer Inhalt'

# author edits a blog of someone else (blog 2 belongs to alice): 403
http -A bearer -a "$BOB" PATCH :8080/blogs/2 content=Gekapert

# author must not delete, not even their own blog: 403
http -A bearer -a "$BOB" DELETE :8080/blogs/<id>

# admin edits any blog: 200
http -A bearer -a "$ALICE" PATCH :8080/blogs/<id> content='Vom Admin'

# admin deletes any blog: 204
http -A bearer -a "$ALICE" DELETE :8080/blogs/<id>
```
