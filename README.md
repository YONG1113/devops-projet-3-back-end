# DataShare Backend

API REST Spring Boot de l'application DataShare. Elle gère les utilisateurs,
l'authentification JWT, l'envoi et le téléchargement de fichiers, l'historique
des fichiers et leur suppression automatique après expiration.

## Fonctionnalités

- création d'un compte et connexion ;
- authentification stateless avec JWT ;
- upload d'un fichier dans Supabase Storage ;
- conservation des métadonnées dans PostgreSQL ;
- expiration configurable de 1 à 7 jours ;
- mot de passe de téléchargement optionnel, encodé avec BCrypt ;
- téléchargement privé par le propriétaire ou public avec un token ;
- historique et suppression des fichiers de l'utilisateur ;
- nettoyage automatique des fichiers expirés toutes les heures.

## Technologies

- Java 21 ;
- Spring Boot 3.5 ;
- Spring Web, Spring Security et Spring Data JPA ;
- PostgreSQL Supabase ;
- Supabase Storage ;
- JWT avec JJWT ;
- Maven Wrapper ;
- JUnit 5, Mockito, MockMvc, Testcontainers et JaCoCo.

## Prérequis

- JDK 21 ;
- accès à un projet Supabase avec une base PostgreSQL et un bucket Storage ;
- Docker uniquement pour les tests d'intégration ;
- les tables `data_user` et `data_file` créées dans PostgreSQL.

## Configuration

Créer un fichier `.env` à la racine de `back_end` :

```dotenv
DB_HOST=<hôte PostgreSQL fourni par Supabase>
DB_PORT=5432
DB_NAME=postgres
DB_USER=<utilisateur PostgreSQL>
DB_PASSWORD=<mot de passe PostgreSQL>

JWT_SECRET=<clé Base64 d'au moins 32 octets avant encodage>
JWT_EXPIRATION_MS=3600000

SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_SECRET_KEY=<clé serveur Supabase>
SUPABASE_BUCKET=<nom du bucket>
```

`AppConfig` charge uniquement `.env`. Le fichier `.env.local` n'est pas lu par
l'application. Ces deux fichiers sont ignorés par Git et ne doivent jamais
contenir de valeurs destinées à être versionnées.

La connexion JDBC à PostgreSQL utilise `sslmode=require`. La clé Supabase doit
être une clé serveur et ne doit jamais être exposée dans le front-end.

## Démarrage

Depuis le répertoire `back_end` :

```bash
sh mvnw spring-boot:run
```

L'API démarre par défaut sur :

```text
http://localhost:8080
```

Le point d'entrée est `dataShareBackEndApplication`.

## API

| Méthode  | URL                  | JWT requis | Description                                          |
| -------- | -------------------- | ---------- | ---------------------------------------------------- |
| `POST`   | `/api/register`      | Non        | Créer un utilisateur                                 |
| `POST`   | `/api/login`         | Non        | Authentifier un utilisateur et retourner un JWT      |
| `GET`    | `/api/me`            | Oui        | Récupérer l'identifiant de l'utilisateur connecté    |
| `POST`   | `/api/file`          | Oui        | Téléverser un fichier                                |
| `GET`    | `/api/files`         | Oui        | Récupérer les fichiers de l'utilisateur connecté     |
| `GET`    | `/api/file`          | Oui        | Télécharger son fichier avec son `objectPath`        |
| `DELETE` | `/api/file/{fileId}` | Oui        | Supprimer son fichier                                |
| `GET`    | `/api/file/info`     | Non        | Obtenir les informations d'un fichier avec son token |
| `GET`    | `/api/file/download` | Non        | Télécharger un fichier avec son token                |

Les routes protégées attendent le JWT dans l'en-tête :

```http
Authorization: Bearer <jwt>
```

## Expiration des fichiers

`ExpiredFileCleanupScheduler` s'exécute au début de chaque heure. Pour chaque
fichier expiré, il supprime d'abord l'objet de Supabase Storage, puis efface les
métadonnées de stockage. L'identifiant, l'utilisateur, le nom original, la date
d'expiration et le token restent conservés pour l'historique.

En cas d'échec Storage, l'erreur est journalisée et le fichier pourra être
retraité lors de l'exécution suivante.

## Tests

Exécuter les tests et générer le rapport JaCoCo avec :

```bash
sh mvnw clean verify
```

Le rapport HTML est généré dans :

```text
target/site/jacoco/index.html
```

Le seuil minimal configuré est de 80 % de couverture des lignes. Lors de la
dernière vérification documentée, les 87 tests ont réussi et la couverture des
lignes était de 92,69 %.

Les tests d'intégration démarrent un PostgreSQL 17 temporaire avec
Testcontainers. Ils nécessitent Docker, ne se connectent pas à Supabase et ne
modifient donc pas la base distante.

## Build

Créer le fichier JAR avec :

```bash
sh mvnw clean package
```

Le résultat est placé dans le répertoire `target/`.

## Supervision

Spring Boot Actuator est activé. L'état de l'application est disponible avec :

```http
GET /actuator/health
```

La configuration actuelle expose tous les endpoints Actuator. Avant une mise
en production, il est recommandé de ne rendre public que `health` et de
protéger ou désactiver les autres endpoints.
