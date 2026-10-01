# DataShare Backend

Backend Spring Boot pour l'inscription des utilisateurs et l'authentification JWT.

## Configuration et démarrage

Prérequis : JDK 21 et une base PostgreSQL distante (Supabase).
Le démarrage ne dépend plus de Docker Compose.

Configurer les paramètres suivants dans `.env`, lu par `AppConfig` :

```dotenv
DB_HOST=<hôte PostgreSQL fourni par Supabase>
DB_PORT=5432
DB_NAME=postgres
DB_USER=<utilisateur fourni par Supabase>
DB_PASSWORD=<mot de passe de la base>
JWT_SECRET=<clé Base64 d'au moins 32 octets avant encodage>
JWT_EXPIRATION_MS=3600000
```

Le fichier `.env.local` n'est pas chargé par la configuration actuelle.
La connexion JDBC utilise SSL. La table `data_user` doit être créée au préalable :
`spring.jpa.hibernate.ddl-auto=validate` vérifie la structure sans la modifier.

Depuis le répertoire `Back-end` :

```sh
sh mvnw spring-boot:run
```

Point d'entrée : `dataShareBackEndApplication`. Port HTTP par défaut : 8080.

## API

- `POST /api/register` : création d'un utilisateur.
- `POST /api/login` : authentification et génération d'un JWT.

## Tests

```sh
sh mvnw clean verify
```

Les tests d'intégration conservent leur base MySQL temporaire via Testcontainers
et nécessitent Docker. Le pilote MySQL est limité au périmètre de test.
Ces tests ne se connectent pas à Supabase.
