# 🛰️ Sky Visibility API

API REST em Java + Spring Boot que busca satélites e planetas visíveis no céu uma vez por dia e salva no MySQL.

## Tecnologias

- Java 17 + Spring Boot 3.3
- Spring Data JPA + MySQL
- WebClient (chamadas HTTP)
- Lombok

## Como rodar

**1. Configure o `application.yml`:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/skydb?createDatabaseIfNotExist=true
    username: root
    password: sua_senha

app:
  location:
    latitude:  -3.3936
    longitude: -44.3669
    city: Itapecuru-Mirim
  apis:
    n2yo:
      api-key: SEU_KEY_N2YO
    solar-system:
      token: SEU_TOKEN_SOLAR
```

**2. Rode:**
```bash
./mvnw spring-boot:run
```

## Endpoints

| Método | URL | Descrição |
|--------|-----|-----------|
| `GET`  | `/api/sky/today` | Dados do dia (lê do banco) |
| `GET`  | `/api/sky/history?days=7` | Histórico dos últimos N dias |
| `POST` | `/api/sky/admin/fetch` | Força busca manual nas APIs |

## Como funciona

O scheduler roda todo dia à meia-noite, busca os dados nas APIs [N2YO](https://www.n2yo.com/api/) e [Le Système Solaire](https://api.le-systeme-solaire.net) e salva no banco. O frontend só lê do banco — sem chamar APIs externas diretamente.
