# 🛰️ Sky Ita API

API REST que responde a uma pergunta simples: **o que está passando no céu de Itapecuru-Mirim/MA hoje?**

A aplicação consulta diariamente fontes astronômicas externas (satélites em órbita e posição dos planetas), consolida o resultado em um *snapshot* diário e o persiste no MySQL. Quem consome a API — um front-end, um app ou um painel — lê apenas do banco, nunca das APIs externas.

---

## 📑 Índice

- [Por que ele existe](#-por-que-ele-existe)
- [Como funciona](#-como-funciona)
- [Tecnologias](#-tecnologias)
- [Estrutura do projeto](#-estrutura-do-projeto)
- [Como rodar](#-como-rodar)
- [Endpoints](#-endpoints)
- [Padrão de desenvolvimento](#-padrão-de-desenvolvimento)
- [Status atual](#-status-atual)
- [Licença](#-licença)

---

## 🎯 Por que ele existe

As APIs públicas de astronomia (N2YO e Le Système Solaire) têm limite de requisições, latência alta e podem ficar indisponíveis. Expor essas APIs diretamente para o cliente significaria:

- estourar a cota de requisições em poucos acessos;
- vazar chaves de API para o front-end;
- ficar refém da disponibilidade de terceiros.

O Sky Ita API resolve isso atuando como **camada de cache e agregação**: busca os dados **uma vez por dia**, normaliza tudo em um formato próprio (`SkySnapshot`) e serve esse conteúdo com resposta rápida e previsível — inclusive quando as APIs de origem estão fora do ar.

O escopo é geolocalizado: as coordenadas de Itapecuru-Mirim/MA são configuráveis em `application.yaml`.

| Parâmetro | Valor padrão |
|-----------|--------------|
| Cidade    | Itapecuru-Mirim |
| Latitude  | `-3.3936` |
| Longitude | `-44.3669` |
| Altitude  | `30` m |

---

## ⚙️ Como funciona

```
┌──────────────┐         ┌─────────────────────────────┐         ┌──────────────┐
│   N2YO API   │────┐    │        Sky Ita API          │         │              │
│  (satélites) │    │    │                             │         │   Cliente    │
└──────────────┘    ├───▶│  SatelliteService           │         │ (front/app)  │
                    │    │  PlanetaService             │         │              │
┌──────────────┐    │    │        │                    │         └──────┬───────┘
│ Le Système   │────┘    │        ▼                    │                │
│   Solaire    │         │  SkyDataService ──▶ MySQL   │◀───────────────┘
│  (planetas)  │         │  (agrega e persiste)        │   GET /api/sky/today
└──────────────┘         └─────────────────────────────┘
   1x por dia                                              lê apenas do banco
```

**Fluxo de escrita (agendado):** uma vez por dia o `SkyDataService.fetchAndSave()` dispara as consultas às duas APIs externas via `WebClient`, converte as respostas para os DTOs internos (`SatelliteResponseDto`, `PlanetResponseDto`), serializa as listas em JSON e grava um registro em `sky_snapshots` — único por data (`snapshot_date` é `UNIQUE`).

**Fluxo de leitura (sob demanda):** os endpoints consultam apenas o `SkySnapshotRepository`. Nenhuma chamada externa acontece durante a requisição do cliente.

**Tratamento de falhas:** erros das APIs externas viram `ExternalApiException`, capturada pelo `GlobalExceptionHandler` (`@RestControllerAdvice`), que padroniza a resposta de erro. Toda chamada externa tem *timeout* de 10 segundos.

### Fontes de dados

| Fonte | Uso | Autenticação |
|-------|-----|--------------|
| [N2YO](https://www.n2yo.com/api/) | Satélites visíveis acima das coordenadas | `API_KEY_N2YO` |
| [Le Système Solaire](https://api.le-systeme-solaire.net) | Dados dos planetas do sistema solar | `TOKEN_SOLAR_SY` |

---

## 🧰 Tecnologias

| Camada | Stack |
|--------|-------|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Web | Spring Web MVC (endpoints) + Spring WebFlux `WebClient` (consumo externo) |
| Persistência | Spring Data JPA + Hibernate + MySQL |
| Validação | Spring Boot Starter Validation (Jakarta Bean Validation) |
| Boilerplate | Lombok |
| Build | Maven (via `mvnw` wrapper) |

---

## 📁 Estrutura do projeto

```
src/main/java/io/github/skyita/
├── SkyitaApplication.java          # entrypoint Spring Boot
├── config/
│   └── WebClientConfig.java        # bean do WebClient.Builder
├── entity/
│   └── SkySnapshot.java            # entidade JPA (tabela sky_snapshots)
├── repository/
│   └── SkySnapshotRepository.java  # JpaRepository + queries derivadas
├── service/
│   ├── SatelliteService.java       # contrato: buscar satélites
│   ├── PlanetaService.java         # contrato: buscar planetas
│   ├── SkyDataService.java         # contrato: orquestrar e persistir
│   └── impl/                       # implementações concretas
├── dto/response/                   # records de saída da API
└── exception/
    ├── ExternalApiException.java
    └── handler/GlobalExceptionHandler.java
```

### Convenções de arquitetura

O projeto segue **arquitetura em camadas com programação por interface**:

- **Interface + `impl`** — todo serviço tem um contrato (`SatelliteService`) e uma implementação (`impl/SatelliteServiceImpl`). Isso permite trocar a fonte de dados ou criar *mocks* em teste sem tocar em quem consome.
- **DTOs são `record`s imutáveis** — nunca se expõe a entidade JPA diretamente na resposta HTTP. A entidade é detalhe de persistência; o DTO é o contrato público.
- **Injeção por construtor** — via `@RequiredArgsConstructor` do Lombok com campos `final`, nunca `@Autowired` em campo.
- **Exceções centralizadas** — nenhum `try/catch` de apresentação nos serviços; erros sobem como exceção e são traduzidos em resposta HTTP pelo `GlobalExceptionHandler`.
- **Segredos fora do código** — todas as credenciais vêm de variáveis de ambiente (`${DB_PASSWORD}`, `${API_KEY_N2YO}`…), nunca literais no `application.yaml`.

---

## 🚀 Como rodar

### Pré-requisitos

- **JDK 21** ou superior
- **MySQL 8+** em execução
- Chave da API [N2YO](https://www.n2yo.com/api/) (cadastro gratuito)
- Token da API [Le Système Solaire](https://api.le-systeme-solaire.net)

### 1. Clonar o repositório

```bash
git clone git@github.com:Attonic/skyita-api.git
cd skyita-api
```

### 2. Criar o banco de dados

O `application.yaml` usa `ddl-auto: validate`, ou seja, **o Hibernate não cria as tabelas** — ele apenas valida se o esquema bate com as entidades. Crie a estrutura antes de subir a aplicação:

```sql
CREATE DATABASE IF NOT EXISTS skydb
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE skydb;

CREATE TABLE sky_snapshots (
    sky_snapshot_id BINARY(16)   NOT NULL,
    snapshot_date   DATE         NOT NULL UNIQUE,
    generated_at    DATETIME(6)  NOT NULL,
    city            VARCHAR(255) NOT NULL,
    latitude        DOUBLE       NOT NULL,
    longitude       DOUBLE       NOT NULL,
    satellites_json TEXT,
    planets_json    TEXT,
    planet_count    INT,
    PRIMARY KEY (sky_snapshot_id)
);
```

> 💡 Durante o desenvolvimento local você pode trocar para `ddl-auto: update` e deixar o Hibernate gerar o esquema. **Nunca** faça isso em produção.

### 3. Configurar as variáveis de ambiente

A aplicação lê cinco variáveis. Exporte-as no shell ou configure-as na *run configuration* da IDE:

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `DB_URL` | JDBC URL do MySQL | `jdbc:mysql://localhost:3306/skydb` |
| `DB_USER_NAME` | Usuário do banco | `root` |
| `DB_PASSWORD` | Senha do banco | `sua_senha` |
| `API_KEY_N2YO` | Chave da API N2YO | `ABCDEF-123456-GHIJKL-1A2B` |
| `TOKEN_SOLAR_SY` | Token do Le Système Solaire | `seu_token` |

```bash
export DB_URL="jdbc:mysql://localhost:3306/skydb"
export DB_USER_NAME="root"
export DB_PASSWORD="sua_senha"
export API_KEY_N2YO="sua_chave_n2yo"
export TOKEN_SOLAR_SY="seu_token_solar"
```

> ⚠️ Se qualquer uma dessas variáveis estiver ausente, a aplicação falha ao subir. Segredos **não** devem ser commitados — use variáveis de ambiente ou um `.env` fora do controle de versão.

### 4. Executar

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

### Outros comandos úteis

```bash
./mvnw clean install     # build completo com testes
./mvnw test              # apenas os testes
./mvnw clean package     # gera o JAR em target/
java -jar target/skyita-0.0.1-SNAPSHOT.jar
```

---

## 🔌 Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET`  | `/api/sky/today` | Snapshot do dia atual (lido do banco) |
| `GET`  | `/api/sky/history?days=7` | Histórico dos últimos N dias |
| `POST` | `/api/sky/admin/fetch` | Dispara manualmente a busca nas APIs externas |

### Exemplo de resposta — `GET /api/sky/today`

```json
{
  "id": "3f9c1a2e-7b4d-4e21-9f88-0a1b2c3d4e5f",
  "snapshotDate": "2026-07-26",
  "generatedAt": "2026-07-26T00:00:12.482",
  "city": "Itapecuru-Mirim",
  "latitude": -3.3936,
  "longitude": -44.3669,
  "satellites": [
    {
      "noradId": 25544,
      "name": "SPACE STATION",
      "launchDate": "1998-11-20",
      "latitude": -2.8471,
      "longitude": -43.9012,
      "altitudeKm": 419.32
    }
  ],
  "planets": [
    {
      "name": "Marte",
      "aximuthDegrees": 112.4,
      "altitudeDegress": 38.7,
      "visible": 1
    }
  ],
  "satelliteCount": 1,
  "planetCount": 1
}
```

### Formato de erro

Todas as exceções passam pelo `GlobalExceptionHandler` e retornam um corpo padronizado:

```json
{
  "erro": "Erro interno. Tente novamente mais tarde.",
  "timesTamo": "2026-07-26T14:03:51.220Z"
}
```

Erros de validação (`@Valid`) retornam `400` com um mapa `campo → mensagem`.

---

## 🔀 Padrão de desenvolvimento

O projeto adota um fluxo **Git Flow simplificado**, com duas branches permanentes e branches temporárias por demanda.

### Branches permanentes

| Branch | Papel | Regras |
|--------|-------|--------|
| `main` | Código estável, pronto para produção. Cada merge aqui representa uma release. | Protegida. Recebe merge **somente** de `development` (ou de `hotfix/*` em emergência). Nunca commite direto. |
| `development` | Branch de integração. Reúne tudo que já foi revisado e será liberado na próxima release. | Protegida. Recebe merge **somente** via Pull Request aprovado. Base de toda branch de trabalho. |

### Branches temporárias

Toda tarefa nasce de uma **issue** e vira uma branch criada **a partir de `development`**:

| Prefixo | Uso | Exemplo |
|---------|-----|---------|
| `feature/` | Nova funcionalidade (US ou Task) | `feature/42-endpoint-sky-today` |
| `fix/` | Correção de bug não urgente | `fix/57-timeout-n2yo` |
| `hotfix/` | Correção urgente em produção (sai de `main`) | `hotfix/61-conexao-mysql` |
| `chore/` | Build, configuração, dependências, infra | `chore/12-atualiza-spring-boot` |
| `docs/` | Documentação | `docs/70-readme-arquitetura` |
| `spike/` | Investigação técnica com timebox | `spike/38-lib-calculo-azimute` |

> Convenção do nome: `<prefixo>/<número-da-issue>-<descrição-curta-em-kebab-case>`.

### Fluxo de trabalho, passo a passo

```
                          ┌──────────────────┐
   (1) issue criada  ───▶ │  development     │ ◀── base de toda branch
                          └────────┬─────────┘
                                   │ (2) git checkout -b feature/42-...
                                   ▼
                          ┌──────────────────┐
                          │ feature/42-...   │  (3) commits pequenos e descritivos
                          └────────┬─────────┘
                                   │ (4) push + Pull Request → development
                                   ▼
                          ┌──────────────────┐
                          │   code review    │  (5) ≥ 1 aprovação + CI verde
                          └────────┬─────────┘
                                   │ (6) merge (branch deletada)
                                   ▼
                          ┌──────────────────┐
                          │  development     │
                          └────────┬─────────┘
                                   │ (7) fim da sprint: PR development → main
                                   ▼
                          ┌──────────────────┐
                          │      main        │  (8) tag de release  v1.0.0
                          └──────────────────┘
```

**1. Tudo começa por uma issue.** Nada é implementado sem issue aberta usando um dos templates do repositório.

**2. Crie a branch a partir de `development` atualizado:**

```bash
git checkout development
git pull origin development
git checkout -b feature/42-endpoint-sky-today
```

**3. Commits pequenos e com propósito único.** O padrão em uso segue o estilo *Conventional Commits* em português:

```
Feat: adiciona endpoint GET /api/sky/today
Fix: corrige timeout na chamada da API N2YO
Docs: atualiza README com fluxo de branches
Refactor: extrai mapeamento de satélite para método próprio
Test: cobre SkyDataService com testes de integração
Chore: atualiza dependência do MySQL connector
```

Regras: prefixo + `:` + descrição no imperativo, em minúscula, sem ponto final. Se o commit fecha uma issue, referencie no corpo (`Closes #42`).

**4. Abra o Pull Request para `development`** (nunca para `main`), com descrição clara do que mudou, a issue vinculada e evidência de teste quando aplicável.

**5. Code review obrigatório.** Pelo menos **1 aprovação** de outro membro antes do merge. Sem aprovação, sem merge.

**6. Merge e limpeza.** Após aprovado e com CI verde, faça o merge e delete a branch de trabalho.

**7. Release.** Ao fim da sprint, `development` é mergeada em `main` e a versão é tagueada (`v1.0.0`, `v1.1.0`…).

### Gestão de issues

O repositório traz templates prontos em `.github/ISSUE_TEMPLATE/`, refletindo uma hierarquia ágil:

| Template | Quando usar |
|----------|-------------|
| 🗂️ **Épico** | Módulo grande demais para uma sprint; agrupa várias User Stories |
| 📖 **User Story** | Requisito funcional na ótica do usuário: *Como [perfil], quero [ação], para [benefício]* |
| ✅ **Task** | Unidade técnica derivada de uma US; deve caber em ~1 dia de trabalho |
| 🐛 **Bug** | Comportamento incorreto, com passos de reprodução e severidade |
| 🔬 **Spike** | Investigação técnica com *timebox*; entrega conhecimento documentado, não código |
| ⚙️ **Tech Debt** | Solução subótima aceita conscientemente e que precisa ser revisada |

**Hierarquia:** `Épico → User Story → Task`. Um bug ou débito técnico pode referenciar a US que o originou.

**Priorização** por labels: `priority:high`, `priority:medium`, `priority:low`.
**Sprints** são representadas por *milestones* do GitHub.
**Estimativas** de User Story em *story points* (Fibonacci: 1, 2, 3, 5, 8, 13).

### Definition of Done

Uma issue só vai para **Done** quando todos os itens estiverem cumpridos:

- [ ] Código implementado conforme a descrição e os critérios de aceite
- [ ] Testes escritos e passando (unitários e/ou integração)
- [ ] Pull Request aberto com descrição clara das mudanças
- [ ] Code review feito por pelo menos 1 membro
- [ ] Mergeado em `development` sem conflitos
- [ ] Critérios de aceite validados pelo PO (para User Stories)
- [ ] Nenhum débito técnico não documentado introduzido

---

## 📌 Status atual

O projeto está em desenvolvimento ativo. Estado das camadas:

| Item | Situação |
|------|----------|
| Entidade `SkySnapshot` + repositório | ✅ Implementado |
| `WebClientConfig` | ✅ Implementado |
| DTOs de resposta | ✅ Implementados |
| Tratamento global de exceções | ✅ Implementado |
| Interfaces de serviço | ✅ Definidas |
| `SatelliteServiceImpl` (N2YO) | ✅ Implementado |
| `PlanetServiceImpl` (Le Système Solaire) | 🚧 Esqueleto — retorna lista vazia |
| `SkyDataServiceImpl` (orquestração/persistência) | 🚧 Pendente |
| Controllers REST (`/api/sky/**`) | 🚧 Pendentes |
| Scheduler diário (`@Scheduled`) | 🚧 Pendente |
| Cobertura de testes | 🚧 Pendente |

Os endpoints documentados acima descrevem o **contrato-alvo** da API e serão expostos conforme as camadas restantes forem concluídas.

---

## 📄 Licença

Distribuído sob a licença **MIT**. Veja [LICENSE](LICENSE) para o texto completo.

---

<p align="center">
  Feito com ☕ e curiosidade sobre o céu de Itapecuru-Mirim/MA
</p>
