# SimuTalk Backend
[![CI](https://github.com/simutalk-upc/simutalk-backend/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/simutalk-upc/simutalk-backend/actions/workflows/ci.yml)
API REST de **SimuTalk**, plataforma de preselección de talento: cada empresa define criterios de evaluación
ponderados para su vacante, el sistema entrevista de forma asincrónica al postulante, puntúa cada respuesta con NLP
y devuelve un ranking donde cada puntaje se rastrea hasta el fragmento textual que lo sustenta.

Curso 1ASI0705 Arquitectura de Aplicaciones Web — UPC — ciclo 202620.

## Stack

Java 21 · Spring Boot 4.1 · Spring Data JPA + Hibernate · PostgreSQL 16 · Spring Security + JWT · springdoc OpenAPI 3 · Maven

La arquitectura (capas horizontales en once paquetes planos), las reglas del proyecto y las convenciones están en [CLAUDE.md](CLAUDE.md).

## Requisitos

- JDK 21
- PostgreSQL 16 en ejecución, con la base `simutalk_db` creada
- No hace falta instalar Maven: se usa el wrapper `./mvnw` (`mvnw.cmd` en Windows)

## Configuración

1. Crea el usuario y la base en PostgreSQL (como superusuario, p. ej. `psql -U postgres`):

   ```sql
   CREATE USER simutalk WITH PASSWORD 'change-me';
   CREATE DATABASE simutalk_db OWNER simutalk;
   ```

2. Copia `.env.example` a `.env` y completa `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`
   (genéralo con `openssl rand -base64 32`; en Windows, con el comando de PowerShell de la
   [guía para Windows](#guía-paso-a-paso-en-windows-con-intellij-idea)). Spring Boot lee `.env` desde la raíz del
   proyecto. También puedes exportar las variables de entorno directamente (o configurarlas en IntelliJ en
   *Run Configuration → Environment variables*).

| Variable | Obligatoria | Por defecto |
|---|---|---|
| `DB_USERNAME` | sí | — |
| `DB_PASSWORD` | sí | — |
| `JWT_SECRET` | sí | — (Base64, ≥ 256 bits) |
| `JWT_EXPIRATION_DAYS` | no | `7` |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | no | — (si ambos existen, crea el admin inicial) |
| `CREDENTIALS_MODE` | no | `mock` (`live` llama al emisor; aún sin emisores conectados) |
| `AI_MODE` | no | `mock` (`live` usa Google Gemini) |
| `GEMINI_API_KEY` | solo en `live` | — |
| `GEMINI_MODEL` | no | `gemini-2.5-flash` |
| `MAIL_MODE` | no | `mock` (solo registra en el log; `live` envía con Brevo) |
| `BREVO_API_KEY` | solo en `live` | — |
| `MAIL_SENDER_EMAIL` | solo en `live` | remitente verificado en Brevo |
| `MAIL_SENDER_NAME` | no | `SimuTalk` |
| `ANONYMIZATION_SECRET` | no | derivado de `JWT_SECRET` (clave de los códigos `CANDIDATO-X-9999`) |
| `SUSTAINABILITY_EMISSION_FACTOR` | no | `0.121` kg CO2e/km (valor de referencia a validar) |
| `SEED_DEMO_DATA` | no | `false` |
| `DEMO_USERS_PASSWORD` | no | — (contraseña de los usuarios demo; sin ella no pueden iniciar sesión) |
| `DB_HOST` | no | `localhost` |
| `DB_PORT` | no | `5432` |
| `SERVER_PORT` | no | `8080` |

## Guía paso a paso en Windows con IntelliJ IDEA

Para quien corre el proyecto por primera vez en su computadora. Requiere IntelliJ IDEA y PostgreSQL
(con pgAdmin 4, que viene en el instalador). Java 21 se puede descargar desde IntelliJ en el paso 2.

### 1. Abrir el proyecto

1. Descomprime el proyecto (o clónalo) en una carpeta, por ejemplo el Escritorio.
2. En IntelliJ, desde la pantalla de bienvenida: **Open** → selecciona la carpeta `simutalk-backend`,
   **la que contiene `pom.xml`** (no una carpeta superior) → **OK** → **Trust Project**.
3. Espera a que termine la barra de progreso de abajo a la derecha: IntelliJ descarga las dependencias
   de Maven. La primera vez puede tardar varios minutos.

> Si abres una carpeta superior, IntelliJ no reconoce el proyecto Maven y no aparece la flecha verde
> para ejecutarlo. En ese caso, clic derecho sobre `pom.xml` → **Add as Maven Project**.

### 2. Configurar Java 21

El proyecto **debe compilarse con Java 21**. Con una versión más nueva, Lombok falla y la compilación
muestra `java.lang.ExceptionInInitializerError ... com.sun.tools.javac.tree.EndPosTable`.

1. Menú **☰ → File → Project Structure... → Project**.
2. En **SDK** elige una versión **21**. Si no aparece, elige **Download JDK...** → Version **21** →
   Vendor **Eclipse Temurin** → **Download**.
3. En **Language level** elige **21**.
4. En **Modules → simutalk → Dependencies**, **Module SDK** debe ser **Project SDK (21)**.
5. **Apply** → **OK**.

### 3. Crear el usuario y la base de datos en pgAdmin

1. Abre **pgAdmin 4** → **Servers → PostgreSQL → Databases**.
2. Clic derecho sobre la base **postgres** → **Query Tool**. Pega lo siguiente, reemplazando
   `TU_CONTRASEÑA`, y ejecútalo con **F5**:

   ```sql
   CREATE USER simutalk WITH PASSWORD 'TU_CONTRASEÑA';
   CREATE DATABASE simutalk_db OWNER simutalk;
   ```

   Si el usuario `simutalk` ya existe, ejecuta solo la segunda línea.
3. Es importante que la base pertenezca a `simutalk` (`OWNER simutalk`). Si la creaste desde el menú de
   pgAdmin con otro dueño, la aplicación no podrá crear las tablas. Para corregirlo, abre el
   **Query Tool** sobre `simutalk_db` y ejecuta:

   ```sql
   ALTER DATABASE simutalk_db OWNER TO simutalk;
   ALTER SCHEMA public OWNER TO simutalk;
   GRANT ALL ON SCHEMA public TO simutalk;
   ```

### 4. Crear el archivo `.env`

1. Genera la clave JWT. En la **Terminal** de IntelliJ (PowerShell) ejecuta y copia el resultado:

   ```powershell
   [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
   ```

   El comando `openssl rand -base64 32` no existe en PowerShell; sí funciona en **Git Bash**.
2. En el panel **Project**, clic derecho sobre `.env.example` → **Copy**, y luego clic derecho sobre
   `simutalk-backend` → **Paste** con el nombre `.env`.
3. En `.env`, completa como mínimo:

   ```properties
   DB_USERNAME=simutalk
   DB_PASSWORD=TU_CONTRASEÑA
   JWT_SECRET=LA_CLAVE_GENERADA
   ```

   Para tener datos de prueba (la empresa `consultora.andina` y seis candidatos), agrega también
   `SEED_DEMO_DATA=true` y una contraseña en `DEMO_USERS_PASSWORD`.
4. Guarda con **Ctrl + S**. El archivo `.env` está en `.gitignore`: nunca se sube al repositorio.

### 5. Ejecutar

1. Pulsa la **flecha verde ▶** junto a **SimutalkApplication** (arriba) o junto a la clase en
   `src/main/java/pe/upc/simutalk/SimutalkApplication.java`.
2. Espera el mensaje `Started SimutalkApplication` en la consola.
3. Abre http://localhost:8080/swagger-ui.html. Si activaste los datos de prueba, inicia sesión en
   `POST /api/v1/authentication/sign-in` con `consultora.andina` y la contraseña de `DEMO_USERS_PASSWORD`.

### Problemas frecuentes

| Mensaje | Causa | Solución |
|---|---|---|
| `ExceptionInInitializerError ... EndPosTable` | El proyecto usa una versión de Java distinta de 21 | Paso 2 |
| `relation "roles" does not exist` o `permission denied for schema public` | El usuario `simutalk` no puede crear tablas en la base | Paso 3, punto 3 |
| `password authentication failed for user "simutalk"` | `DB_PASSWORD` no coincide con la contraseña del usuario | Revisar `.env` y el paso 3 |
| `Could not resolve placeholder 'JWT_SECRET'` | Falta el `.env` o no está en la carpeta del `pom.xml` | Paso 4 |
| `Port 8080 was already in use` | Otra aplicación usa el puerto 8080 | Detenerla con el botón ■ o cambiar `SERVER_PORT` en `.env` |
| No aparece la flecha verde para ejecutar | Se abrió una carpeta distinta a la del `pom.xml` | Paso 1 |

## Ejecución

```bash
./mvnw spring-boot:run        # Linux / macOS
mvnw.cmd spring-boot:run      # Windows
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Pruebas:

```bash
./mvnw test
```

## Ejecución con Docker

Alternativa que no requiere instalar JDK 21 ni PostgreSQL: solo [Docker Desktop](https://www.docker.com/products/docker-desktop/).

1. Copia `.env.example` a `.env` y completa al menos `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`.
   No hace falta cambiar `DB_HOST`: el `docker-compose.yml` lo reemplaza por `db`, el nombre del
   contenedor de PostgreSQL.
2. Levanta la API y la base de datos:

   ```bash
      docker compose up --build
   ```

   La primera ejecución tarda varios minutos porque descarga las imágenes y las dependencias de Maven.
   La API está lista cuando el log muestra `Started SimutalkApplication`.
3. Abre Swagger UI en http://localhost:8080/swagger-ui.html
4. Para detenerlo: `Ctrl + C` y luego `docker compose down`. Los datos se conservan en el volumen
   `pgdata`; para borrarlos usa `docker compose down -v`.

| Archivo | Propósito |
|---|---|
| `Dockerfile` | Build en dos etapas: compila con Maven (JDK 21) y ejecuta solo el `.jar` sobre una imagen JRE 21 |
| `docker-compose.yml` | Levanta `db` (PostgreSQL 16 con la base `simutalk_db`) y `api`, que espera a que la base esté lista |
| `.dockerignore` | Evita que `.env`, `target/`, `.idea/` y `.git/` entren a la imagen |

## Autenticación

1. `POST /api/v1/authentication/sign-up` con `{"username", "password", "roles": ["ROLE_RECRUITER"]}`
   (roles permitidos: `ROLE_CANDIDATE`, `ROLE_RECRUITER`; sin roles = `ROLE_CANDIDATE`).
2. `POST /api/v1/authentication/sign-in` → devuelve `token`.
3. En Swagger UI pulsa **Authorize** (candado), pega el token (sin el prefijo `Bearer`) y prueba el resto.

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/v1/authentication/sign-up` | Público |
| POST | `/api/v1/authentication/sign-in` | Público |
| GET | `/api/v1/users` | `ROLE_ADMIN` |
| GET | `/api/v1/users/{userId}` | `ROLE_ADMIN` o el propio usuario |
| GET | `/api/v1/roles` | Autenticado |

## Perfiles y certificaciones (área de perfiles, requieren token)

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/v1/company-profiles` | Recruiter (el suyo) o admin |
| GET | `/api/v1/company-profiles/me` | El usuario autenticado (404 si no tiene perfil de empresa) |
| GET | `/api/v1/company-profiles?page=&size=` | Admin (paginado, `page` desde 0, `size` 1 a 100, 20 por defecto) |
| GET | `/api/v1/company-profiles/{id}` | Autenticado |
| PUT | `/api/v1/company-profiles/{id}` | Dueño o admin |
| POST | `/api/v1/candidate-profiles` | Candidato (el suyo) o admin |
| GET | `/api/v1/candidate-profiles/me` | El usuario autenticado (404 si no tiene perfil de postulante) |
| GET | `/api/v1/candidate-profiles?page=&size=` | Admin (paginado, `page` desde 0, `size` 1 a 100, 20 por defecto) |
| GET | `/api/v1/candidate-profiles/{id}` | Dueño, recruiter o admin |
| PUT | `/api/v1/candidate-profiles/{id}` | Dueño o admin |
| POST | `/api/v1/candidate-profiles/{id}/certifications` | Dueño o admin (nace UNVERIFIED) |
| GET | `/api/v1/candidate-profiles/{id}/certifications` | Dueño, recruiter o admin |
| POST | `/api/v1/candidate-profiles/{id}/certifications/{certificationId}/verification` | Dueño o admin |
| DELETE | `/api/v1/candidate-profiles/{id}/certifications/{certificationId}` | Dueño o admin |

Los listados paginados responden `{"content": [...], "page", "size", "totalElements", "totalPages"}`.

Datos demo: arranca con `SEED_DEMO_DATA=true` y `DEMO_USERS_PASSWORD=...`. Usuarios: `consultora.andina` (recruiter),
`rosa.quispe`, `jorge.huaman`, `lucia.flores`, `miguel.condori`, `carmen.ramos`, `diego.salazar` (candidatos).

## Postulaciones (área de reclutamiento, requieren token)

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/v1/job-postings/{jobPostingId}/applications` | Candidato (su propio perfil); solo vacantes PUBLISHED, una vez |
| GET | `/api/v1/job-postings/{jobPostingId}/applications?status=` | Recruiter dueño o admin (pipeline del dashboard) |
| GET | `/api/v1/applications` | Candidato: sus postulaciones |
| PATCH | `/api/v1/applications/{applicationId}/status` | Recruiter dueño o admin; transiciones dirigidas |

Etapas: `RECEIVED → INTERVIEWING → ASSESSED → SHORTLISTED → HIRED`, y `REJECTED` desde cualquier etapa no final.
Un salto inválido responde 422.

## Notificaciones por correo al candidato (área de reclutamiento)

No tienen endpoint propio: el correo sale solo cuando una postulación cambia a una de estas etapas.

| Etapa nueva | Correo | Qué la provoca | Quién puede |
|---|---|---|---|
| `INTERVIEWING` | Invitación a entrevista | `POST /api/v1/applications/{applicationId}/interview-session` | Recruiter dueño o admin |
| `SHORTLISTED` | Aviso de terna final | `PATCH /api/v1/applications/{applicationId}/status` | Recruiter dueño o admin |
| `REJECTED` | Descarte | `PATCH /api/v1/applications/{applicationId}/status` | Recruiter dueño o admin |

`ASSESSED` y `HIRED` no envían correo. El destinatario es el `email` del perfil del candidato; si no tiene, no se
envía y queda un WARN en el log. El correo sale después de guardar el cambio y un fallo del envío nunca lo deshace.

Request (el mismo que mueve la postulación):

```http
PATCH /api/v1/applications/5/status
Authorization: Bearer <token del recruiter>
Content-Type: application/json

{ "status": "SHORTLISTED" }
```

Response `200` (la postulación; el correo no aparece en la respuesta):

```json
{
  "id": 5,
  "jobPostingId": 1,
  "candidateId": 3,
  "status": "SHORTLISTED",
  "appliedAt": "2026-09-28T15:04:11.120Z",
  "createdAt": "2026-09-28T15:04:11.131Z",
  "updatedAt": "2026-09-30T18:22:40.502Z"
}
```

Con `MAIL_MODE=mock` (por defecto) no se envía nada; solo se registra en el log, con la dirección enmascarada:

```
Mail (mock, not sent) SHORTLISTED to r***@example.com: "Avanzaste a la terna final de «Analista de Datos Junior»"
```

Con `MAIL_MODE=live` (requiere `BREVO_API_KEY` y `MAIL_SENDER_EMAIL`) se llama a Brevo con
`POST https://api.brevo.com/v3/smtp/email`, cabecera `api-key` y este cuerpo:

```json
{
  "sender": { "name": "SimuTalk", "email": "seleccion@tu-dominio.pe" },
  "to": [{ "email": "rosa@example.com", "name": "Rosa" }],
  "subject": "Avanzaste a la terna final de «Analista de Datos Junior»",
  "textContent": "Hola, Rosa:\n\n¡Buenas noticias! Formas parte de la terna final para «Analista de Datos Junior». La empresa se pondrá en contacto contigo para los siguientes pasos.\n\nEquipo de selección"
}
```

Asuntos: invitación «Te invitamos a la entrevista para «…»», terna «Avanzaste a la terna final de «…»», descarte
«Actualización de tu postulación a «…»».

## Reportes (área de reportes, requieren token)

| Método | Ruta | Contenido |
|---|---|---|
| GET | `/api/v1/reports/job-postings/{id}/funnel` | Postulaciones por etapa: RECEIVED, INTERVIEWING, ASSESSED, SHORTLISTED, HIRED, REJECTED |
| GET | `/api/v1/reports/job-postings/{id}/criterion-averages` | Puntaje medio por criterio sobre los evaluados |
| GET | `/api/v1/reports/job-postings/{id}/carbon-savings` | kg CO2e evitados, traslados no realizados y km no recorridos |
| GET | `/api/v1/reports/companies/{id}/summary` | Vacantes activas, candidatos evaluados, días promedio hasta la terna y evidencias ancladas |

Acceso: recruiter de la empresa dueña o admin. Todos los valores se calculan con consultas de agregación en la base.

## Evaluación y ranking (área de evaluación, requieren token)

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/v1/interview-sessions/{sessionId}/assessment` | Recruiter dueño o admin; entrevista COMPLETED, una vez |
| GET | `/api/v1/interview-sessions/{sessionId}/assessment` | Recruiter dueño o admin (evaluación completa); candidato dueño de la entrevista (vista reducida, ver abajo) |
| GET | `/api/v1/assessments/{assessmentId}/criterion-scores/{criterionScoreId}/evidences` | Recruiter dueño o admin |
| GET | `/api/v1/job-postings/{jobPostingId}/ranking?anonymized=` | Recruiter dueño o admin |

`weightedScore = Σ(puntaje × peso) / 100`, redondeado a 1 decimal. Cada puntaje de competencia trae su evidencia: el
fragmento literal de la respuesta y su posición. Con `anonymized=true` (o forzado por `anonymizedScreening`) el ranking
muestra `CANDIDATO-X-9999` en lugar de nombre y documento. Al proveedor de IA solo viaja el transcript anonimizado y
el criterio.

## Evaluación vista por el candidato (área de evaluación, requiere token)

| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/v1/interview-sessions/{sessionId}/assessment` | Candidato dueño de la entrevista (`ROLE_CANDIDATE`) |

Es la misma ruta que usa el recruiter, pero el candidato recibe otra vista: su puntaje ponderado, su desglose por
criterio y `feedbackSummary`. No incluye posición en el ranking, puntajes de otros candidatos, señales de integridad,
confianza ni evidencias. Otro candidato recibe 403; si la entrevista aún no fue evaluada, 404.

Request:

```http
GET /api/v1/interview-sessions/1/assessment
Authorization: Bearer <token del candidato>
```

Response `200`:

```json
{
  "interviewSessionId": 1,
  "jobPostingId": 1,
  "weightedScore": 7.5,
  "computedAt": "2026-09-30T18:05:12.345Z",
  "criterionScores": [
    { "criterionName": "Pensamiento analítico", "criterionKind": "COMPETENCY", "weightApplied": 50, "score": 8.2 },
    { "criterionName": "Comunicación efectiva", "criterionKind": "COMPETENCY", "weightApplied": 30, "score": 6.5 },
    { "criterionName": "Certificación en análisis de datos", "criterionKind": "CERTIFICATION", "weightApplied": 20, "score": 7.0 }
  ],
  "feedbackSummary": "Tu puntaje ponderado fue 7,5 de 10. Lo que más sostuvo tu puntaje fue «Pensamiento analítico» (8,2 de 10), donde tus respuestas mostraron evidencia concreta de lo que pide el criterio. Donde más puntos se perdieron fue «Comunicación efectiva» (6,5 de 10, peso 30 %): respuestas con ejemplos concretos de ese criterio lo mejorarían. Cada punto que mejores ahí suma 0,3 a tu puntaje ponderado."
}
```

El `feedbackSummary` del ejemplo es el de `AI_MODE=mock` (texto determinista a partir de los puntajes). Con `live` lo
redacta Gemini a partir de los puntajes, pesos y fragmentos anonimizados; es `null` en evaluaciones calculadas antes
de que existiera la retroalimentación.

## Entrevistas (área de entrevistas, requieren token)

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/v1/job-postings/{jobPostingId}/questions` | Recruiter dueño o admin; solo en DRAFT y sobre criterios COMPETENCY |
| POST | `/api/v1/job-postings/{jobPostingId}/questions/suggestions?criterionId=` | Solo el recruiter dueño; vacante en DRAFT y criterio COMPETENCY. Propone preguntas sin guardarlas |
| GET | `/api/v1/job-postings/{jobPostingId}/questions` | Recruiter dueño, admin o candidato con entrevista IN_PROGRESS en la vacante |
| PUT | `/api/v1/job-postings/{jobPostingId}/questions/{questionId}` | Recruiter dueño o admin; solo en DRAFT |
| DELETE | `/api/v1/job-postings/{jobPostingId}/questions/{questionId}` | Recruiter dueño o admin; solo en DRAFT |
| PATCH | `/api/v1/job-postings/{jobPostingId}/questions/order` | Recruiter dueño o admin; `{"orderedIds": [...]}` con todo el guion |
| POST | `/api/v1/applications/{applicationId}/interview-session` | Recruiter dueño o admin; postulación RECEIVED → INTERVIEWING |
| GET | `/api/v1/applications/{applicationId}/interview-session` | Recruiter dueño, admin o el candidato de la postulación |
| POST | `/api/v1/interview-sessions/{sessionId}/start` | Solo el candidato dueño |
| POST | `/api/v1/interview-sessions/{sessionId}/answers` | Solo el candidato dueño, en IN_PROGRESS |
| POST | `/api/v1/interview-sessions/{sessionId}/completion` | Solo el candidato dueño; postulación → ASSESSED |
| GET | `/api/v1/interview-sessions/{sessionId}/answers` | Candidato dueño, recruiter de la vacante o admin |

Una vacante no se puede publicar mientras algún criterio COMPETENCY no tenga preguntas. Con la vacante publicada, el
guion queda congelado.

Las sugerencias devuelven `[{"criterionId", "statement", "origin": "AI_SUGGESTED", "rationale"}]` y no se guardan:
para aceptar una, envíala a `POST /questions` con `origin=AI_SUGGESTED` y el `maxDurationSeconds` que elijas. Con
`AI_MODE=mock` son 3 preguntas de plantilla derivadas del criterio; con `live` las propone Gemini, que solo recibe
el texto de la vacante, del criterio y del guion (ningún dato de candidatos).

## Endpoints de vacantes (área de reclutamiento, requieren token)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/job-postings` | Crea una vacante en DRAFT a nombre de la empresa del recruiter autenticado |
| GET | `/api/v1/job-postings/{id}` | Obtiene una vacante con sus criterios |
| GET | `/api/v1/job-postings?companyId=&status=` | Lista vacantes (filtros opcionales) |
| PUT | `/api/v1/job-postings/{id}` | Actualiza título, descripción, fecha de cierre y anonimización |
| DELETE | `/api/v1/job-postings/{id}` | Elimina una vacante (no si está PUBLISHED) |
| POST | `/api/v1/job-postings/{id}/criteria` | Agrega un criterio (solo en DRAFT) |
| PUT | `/api/v1/job-postings/{id}/criteria/{criterionId}` | Modifica un criterio (solo en DRAFT) |
| DELETE | `/api/v1/job-postings/{id}/criteria/{criterionId}` | Elimina un criterio (solo en DRAFT) |
| PATCH | `/api/v1/job-postings/{id}/status` | Cambia el estado: `PUBLISHED` (exige pesos = 100) o `CLOSED` |

Escrituras (POST/PUT/DELETE de vacantes y criterios, PATCH de estado): solo el recruiter de la empresa dueña o un
admin; los demás reciben 403. Lecturas: cualquier autenticado, pero un DRAFT solo lo ve su empresa y los listados
muestran de otras empresas solo las PUBLISHED.

## Sugerencia de criterios (área de reclutamiento, requiere token)

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/v1/job-postings/{jobPostingId}/criteria/suggestions` | Solo el recruiter dueño de la vacante; vacante en DRAFT |

Propone hasta 4 criterios COMPETENCY a partir del título y la descripción de la vacante, sin repetir los que ya
tiene. No guarda nada y **no trae peso**: el peso lo pone siempre el reclutador. Un admin u otro recruiter recibe 403;
vacante inexistente, 404; vacante que no está en DRAFT, 422.

Request (sin cuerpo):

```http
POST /api/v1/job-postings/1/criteria/suggestions
Authorization: Bearer <token del recruiter>
```

Response `200` (con `AI_MODE=mock`, para la vacante «Analista de Datos Junior» cuya descripción es «Análisis de datos
comerciales con SQL, Excel y Python; elaboración de reportes para clientes y presentación de hallazgos al equipo
comercial.»):

```json
[
  {
    "name": "Pensamiento analítico",
    "description": "Descompone problemas y los resuelve apoyándose en datos.",
    "criterionType": "COMPETENCY",
    "origin": "AI_SUGGESTED",
    "rationale": "La descripción menciona: datos, analisis, sql, excel, python, reporte"
  },
  {
    "name": "Comunicación efectiva",
    "description": "Explica ideas y hallazgos con claridad a públicos técnicos y no técnicos.",
    "criterionType": "COMPETENCY",
    "origin": "AI_SUGGESTED",
    "rationale": "La descripción menciona: presenta, cliente, reporte"
  },
  {
    "name": "Trabajo en equipo",
    "description": "Colabora con otras personas y áreas para lograr objetivos comunes.",
    "criterionType": "COMPETENCY",
    "origin": "AI_SUGGESTED",
    "rationale": "La descripción menciona: equipo"
  },
  {
    "name": "Orientación al cliente",
    "description": "Entiende y atiende las necesidades del cliente interno o externo.",
    "criterionType": "COMPETENCY",
    "origin": "AI_SUGGESTED",
    "rationale": "La descripción menciona: cliente"
  }
]
```

Para aceptar una sugerencia, envíala a `POST /api/v1/job-postings/{jobPostingId}/criteria` con el peso que elijas:

```json
{
  "name": "Pensamiento analítico",
  "description": "Descompone problemas y los resuelve apoyándose en datos.",
  "weight": 40,
  "criterionType": "COMPETENCY",
  "origin": "AI_SUGGESTED"
}
```

Con `AI_MODE=live` las propone Gemini a partir del texto de la vacante; cualquier peso que devuelva se ignora.

## Errores

Todas las respuestas de error tienen la forma

```json
{
  "timestamp": "2026-09-23T03:52:24.889Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "The weights of the evaluation criteria must add up to exactly 100 (current total: 90)",
  "path": "/api/v1/job-postings/1/status"
}
```

con `details` por campo en los errores de validación (400). 401 = sin token o credenciales inválidas,
403 = sin permiso, 404 = recurso inexistente, 409 = conflicto de unicidad, 422 = regla de negocio.

## Ramas

`main` (estable) · `develop` (integración) · `feature/<area>-<descripcion>`. Commits con
[Conventional Commits](https://www.conventionalcommits.org/). Detalle en [CLAUDE.md](CLAUDE.md).