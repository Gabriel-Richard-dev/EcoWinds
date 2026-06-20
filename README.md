# EcoWinds

Sistema de gestão de ar-condicionado em salas de aula, com importação de horários a partir do SIGEHO.

## Testar o Web Scraper (SIGEHO)

O backend pode buscar horários diretamente do portal SIGEHO (campus Maracanaú) e gravar salas e aulas no banco. Este guia mostra como executar e validar esse fluxo localmente.

### Pré-requisitos

- **Java 25** e **Maven**
- **PostgreSQL** rodando em `localhost:5432`
- Conexão com a internet (o scraper acessa o site do SIGEHO)

### 1. Subir o banco de dados

Na pasta `backend`, com Docker instalado:

```powershell
cd backend
docker compose up -d
```

Sem Docker, use um PostgreSQL local com:

- Banco: `ecowinds_db`
- Usuário: `postgres`
- Senha: `postgres`

As credenciais podem ser alteradas via variáveis `DATABASE_URL`, `PGUSER` e `PGPASSWORD`.

### 2. Iniciar o backend

```powershell
cd backend
mvn spring-boot:run
```

Aguarde a mensagem `Started Startup` nos logs. O servidor sobe na porta **8080**.

### 3. Obter um token JWT

Não há usuário padrão. Registre uma conta (apenas na primeira vez) e faça login:

```powershell
# Registrar (ignore o erro se o e-mail já existir)
Invoke-RestMethod -Uri "http://localhost:8080/auth/register" `
  -Method POST -ContentType "application/json" `
  -Body '{"name":"Seu Nome","email":"seu@email.com","password":"suasenha"}'

# Login
$login = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" `
  -Method POST -ContentType "application/json" `
  -Body '{"email":"seu@email.com","password":"suasenha"}'

$token = $login.token
Write-Host "Token: $token"
```

### 4. Disparar o scrape

**CCM (curso 9) e CCT (curso 18)** — padrão:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/imports/scrape" `
  -Method POST `
  -Headers @{ Authorization = "Bearer $token" } | ConvertTo-Json -Depth 5
```

**Apenas um curso:**

```powershell
# Só CCM (curso 9)
Invoke-RestMethod -Uri "http://localhost:8080/api/imports/scrape?courseIds=9" `
  -Method POST `
  -Headers @{ Authorization = "Bearer $token" } | ConvertTo-Json -Depth 5

# Só CCT (curso 18)
Invoke-RestMethod -Uri "http://localhost:8080/api/imports/scrape?courseIds=18" `
  -Method POST `
  -Headers @{ Authorization = "Bearer $token" } | ConvertTo-Json -Depth 5
```

### 5. O que esperar

**Resposta da API** — um objeto por curso importado:

```json
[
  {
    "status": "SUCCESS",
    "filename": "sigeho-web-c3-s82-course9",
    "source": "WEB_SCRAPER",
    "parserUsed": "sigeho-html",
    "roomsAffected": 14,
    "schedulesCreated": 78,
    "schedulesDeleted": 0,
    "startedAt": "2026-06-19T22:13:18",
    "finishedAt": "2026-06-19T22:13:18"
  }
]
```

**Logs do backend** (terminal do `mvn spring-boot:run`):

```
SIGEHO web scrape starting — campus=maracanau courses=[9, 18]
SIGEHO session opened — campus=maracanau latestSemestre=82
SIGEHO HTML fetched: 529724 bytes (campusId=3 semestre=82 course=9)
SIGEHO scrape courseId=9 → status=SUCCESS rooms=14 schedules=78
SIGEHO scrape courseId=18 → status=SUCCESS rooms=10 schedules=44
```

### 6. Conferir os dados importados

```powershell
$h = @{ Authorization = "Bearer $token" }

# Histórico de importações
Invoke-RestMethod -Uri "http://localhost:8080/api/imports" -Headers $h | ConvertTo-Json -Depth 5

# Salas
Invoke-RestMethod -Uri "http://localhost:8080/room/search?search=a&page=0&size=50" -Headers $h |
  Select-Object -ExpandProperty content | Format-Table identification, block, status

# Horários
Invoke-RestMethod -Uri "http://localhost:8080/class-schedule/search?search=a&page=0&size=100" -Headers $h |
  Select-Object -ExpandProperty content | Format-Table dayOfWeek, startTime, endTime, course, roomIdentification
```

Documentação interativa da API: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Configuração do scraper

Variáveis em `backend/src/main/resources/application.properties` (ou via ambiente):

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `SIGEHO_SCRAPER_ENABLED` | `true` | Habilita o job e o endpoint `/api/imports/scrape` |
| `SIGEHO_CAMPUS_ID` | `3` | ID do campus no SIGEHO |
| `SIGEHO_CAMPUS_SLUG` | `maracanau` | Slug do campus na URL |
| `SIGEHO_COURSE_IDS` | `9,18` | Cursos usados pelo job agendado (CCM e CCT) |
| `SIGEHO_SCRAPER_CRON` | `0 0 3 1 * *` | Cron do scrape automático (dia 1 de cada mês, 03:00) |

Se `SIGEHO_SCRAPER_ENABLED=false`, o endpoint `/api/imports/scrape` retorna **503**.

### Problemas comuns

| Sintoma | Solução |
|---------|---------|
| `Connection to localhost:5432 refused` | Suba o PostgreSQL (`docker compose up -d` na pasta `backend`) |
| `401 Unauthorized` no scrape | Faça login e passe o header `Authorization: Bearer <token>` |
| `503` no scrape | Confirme `SIGEHO_SCRAPER_ENABLED=true` |
| Scrape lento ou timeout | Verifique a conexão com a internet; o SIGEHO pode demorar a responder |
