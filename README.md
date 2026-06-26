# Sistema de Estacionamento (Backend)

Esse projeto consiste em um sistema backend para gerenciar um estacionamento com as seguintes funcionalidades: controle de vagas, processamento de
eventos de entrada/saída de veículos e cálculo de receita por setor.

## Stack

- **Java 21**
- **Spring Boot 3.3** (Web, Data JPA, Validation, Actuator)
- **MySQL 8** (produção) / **H2** (testes)
- **Flyway** (migrations)
- **Maven**

## Arquitetura (resumo)

```
client/        -> cliente REST do simulador (GET /garage) + DTOs
bootstrap/     -> carga inicial da garagem no startup
config/        -> RestClient e propriedades do simulador
domain/        -> entidades JPA (Sector, Spot, ParkingSession)
repository/    -> repositórios Spring Data
service/       -> regras de negócio (Pricing, Parking, Revenue, Bootstrap)
web/           -> controllers REST (webhook, revenue) + DTOs + handler de erros
exception/     -> exceções de domínio
```

## Endpoints

| Método | Rota                              | Descrição                              |
|--------|-----------------------------------|----------------------------------------|
| POST   | `/webhook`                        | Recebe eventos `ENTRY`, `PARKED`, `EXIT` |
| GET    | `/revenue?date=YYYY-MM-DD&sector=A` | Faturamento total do setor na data     |
| GET    | `/actuator/health`                | Health check                           |

---

## Pré-requisitos

- Docker (para o simulador e MySQL)
- JDK 21 e Maven

---

## Passo 1 — Subir o simulador

```bash
docker run -d --network="host" cfontes0estapar/garage-sim:1.0.0
```

O simulador expõe `GET http://localhost:3000/garage` e envia eventos para
`http://localhost:3003/webhook`.

---

## Passo 2 — Subir banco + aplicação

### Opção A — Linux (docker-compose, tudo em containers)

No Linux o `network_mode: host` funciona nativamente:

```bash
docker compose up --build
```

Isso sobe o MySQL (porta 3306 do host) e a aplicação (porta 3003 do host),
que já consegue falar com o simulador em `localhost:3000`.

### Opção B — Windows / macOS

No Docker Desktop o `network_mode: host` é limitado. O caminho mais simples é
subir **apenas o MySQL** em container e rodar a **app pelo Maven** no host:

```bash
# 1) sobe só o MySQL
docker run -d --name estapar-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=estapar \
  -e MYSQL_USER=estapar \
  -e MYSQL_PASSWORD=estapar \
  mysql:8.0

# 2) roda a aplicação (lê localhost:3306 e localhost:3000 por padrão)
mvn spring-boot:run
```

> A configuração padrão (`application.yml`) já aponta para `localhost:3306`
> (MySQL) e `localhost:3000` (simulador), então nenhuma variável extra é
> necessária nessa opção.

---

## Como testar manualmente

Com simulador + app no ar, os eventos chegam sozinhos no webhook. Para testar
manualmente:

```bash
# Consultar a config da garagem (vinda do simulador)
curl http://localhost:3000/garage

# Simular um ENTRY
curl -X POST http://localhost:3003/webhook \
  -H "Content-Type: application/json" \
  -d '{"license_plate":"ABC1234","entry_time":"2025-01-01T12:00:00.000Z","event_type":"ENTRY"}'

# Simular o PARKED (use lat/lng de uma vaga real do /garage)
curl -X POST http://localhost:3003/webhook \
  -H "Content-Type: application/json" \
  -d '{"license_plate":"ABC1234","lat":-23.561684,"lng":-46.655981,"event_type":"PARKED"}'

# Simular o EXIT
curl -X POST http://localhost:3003/webhook \
  -H "Content-Type: application/json" \
  -d '{"license_plate":"ABC1234","exit_time":"2025-01-01T14:00:00.000Z","event_type":"EXIT"}'

# Consultar o faturamento
curl "http://localhost:3003/revenue?date=2025-01-01&sector=A"
```

---

## Testes automatizados

A suite de testes foi desenvolvida com **JUnit 5**, **Mockito** e **Spring Test** para cobrir
regras de negócio, validações, fluxos de integração e manipulação de dados.

### Rodar todos os testes

```bash
mvn test
```

Executa todos os 7 testes da suite. Os testes usam o profile `test` (H2 em memória, 
Flyway e simulador desligados), portanto **não** dependem de MySQL nem do simulador.

### Rodar testes de uma classe específica

```bash
# Apenas testes de precificação
mvn test -Dtest=PricingServiceTest

# Apenas testes de integração do fluxo
mvn test -Dtest=ParkingFlowIntegrationTest

# Apenas testes do serviço de estacionamento
mvn test -Dtest=ParkingServiceUnitTest

# Apenas testes do webhook
mvn test -Dtest=WebhookControllerTest
```

### Rodar um teste específico dentro de uma classe

```bash
# Apenas um método de teste
mvn test -Dtest=PricingServiceTest#dynamicFactorBelow25

# Usando wildcards
mvn test -Dtest=PricingService*
```

### Gerar relatório de cobertura de testes

```bash
mvn clean test jacoco:report
```

O relatório fica em `target/site/jacoco/index.html`. Abre no navegador para ver
a cobertura linha-a-linha de cada classe.

---

## Suites de testes — Descrição detalhada

### 1. **GarageConfigResponseTest**
   - **Tipo:** Unitário (DTO)
   - **Locação:** `src/test/java/com/estapar/parking/client/dto/`
   - **O que testa:** Serialização/desserialização do DTO de resposta da garagem (campos, tipos, conversões)
   - **Cenários:** Parsing JSON da config, campos obrigatórios e opcionais

### 2. **PricingServiceTest** 
   - **Tipo:** Unitário
   - **Locação:** `src/test/java/com/estapar/parking/service/`
   - **O que testa:** Regras de **preço dinâmico** e cálculo de **tarifa horária**
   - **Cenários:**
     - Fator dinâmico **< 25% ocupação** → desconto −10% (fator 0.90)
     - Fator dinâmico **25%–50% ocupação** → neutro (fator 1.00)
     - Fator dinâmico **50%–75% ocupação** → aumento +10% (fator 1.10)
     - Fator dinâmico **≥ 75% ocupação** → aumento +25% (fator 1.25)
     - Setor sem vagas → fator neutro 1.00
     - Cálculo de tarifa: 30 min grátis, acima disso cobra por hora cheia
     - Validações de entrada (preço negativo, durações inválidas)

### 3. **GarageBootstrapServiceTest**
   - **Tipo:** Unitário (com mocks)
   - **Locação:** `src/test/java/com/estapar/parking/service/`
   - **O que testa:** Inicialização e carga de setores/vagas ao startup
   - **Cenários:**
     - Carga bem-sucedida da config do simulador
     - Criação de setores e vagas no banco
     - Idempotência: não duplica dados se rodar novamente
     - Tratamento de erro quando simulador indisponível

### 4. **ParkingServiceUnitTest**
   - **Tipo:** Unitário (com mocks)
   - **Locação:** `src/test/java/com/estapar/parking/service/`
   - **O que testa:** Lógica de processamento de eventos (ENTRY, PARKED, EXIT)
   - **Cenários:**
     - **ENTRY:** recusa se já existe sessão aberta (mesmo prato)
     - **ENTRY:** recusa se estacionamento lotado (409 Conflict)
     - **PARKED:** recusa se setor lotado (409 Conflict)
     - **PARKED:** marca vaga como ocupada, cria sessão
     - **EXIT:** calcula tarifa, libera vaga, fecha sessão
     - **EXIT:** sem evento PARKED prévio encerra a sessão com tarifa zero
     - Validações: placa inválida, vaga inexistente, sessão não encontrada
     - Exceções customizadas (`GarageFullException`, `BusinessException`)

### 5. **WebhookControllerTest**
   - **Tipo:** Unitário (com stub)
   - **Locação:** `src/test/java/com/estapar/parking/web/`
   - **O que testa:** Recepção e roteamento de eventos HTTP no `/webhook`
   - **Cenários:**
     - POST `/webhook` com `event_type: ENTRY` → chama `handleEntry()`
     - POST `/webhook` com `event_type: PARKED` → chama `handleParked()`
     - POST `/webhook` com `event_type: EXIT` → chama `handleExit()`
     - Retorna HTTP 200 OK em sucesso
     - Tratamento de payloads malformados

### 6. **RevenueServiceTest**
   - **Tipo:** Unitário (com mocks)
   - **Locação:** `src/test/java/com/estapar/parking/service/`
   - **O que testa:** Cálculo e agregação de receita por setor e data
   - **Cenários:**
     - Setor não existe → lança `BusinessException`
     - Sem sessões do dia → retorna receita **0.00**
     - Múltiplas sessões → suma corretamente
     - Filtra por data: sessões de outro dia não contam
     - Filtra apenas sessões **finalizadas** (status EXIT)

### 7. **ParkingFlowIntegrationTest**
   - **Tipo:** **Integração** (end-to-end com H2)
   - **Locação:** `src/test/java/com/estapar/parking/web/`
   - **O que testa:** Fluxo completo: **ENTRY → PARKED → EXIT → /revenue** (sem mock)
   - **Ambiente:** 
     - Banco H2 em memória (profile `test`)
     - MockMvc (não faz HTTP real)
     - Setup de dados: cria setores e vagas
   - **Cenários principais:**
     - Fluxo bem-sucedido: entrada → estacionamento → saída → consulta receita
     - Múltiplos veículos: vários carros no mesmo setor
     - Lotação: tenta entrada em setor lotado → HTTP **409 Conflict**
     - Setor inexistente: `/revenue?sector=Z` → HTTP **422 Unprocessable Entity**
     - Preço dinâmico aplicado: ocupação afeta tarifa final
     - Dados persistem: entrada cria, saída atualiza no banco

---

## Executar testes com saída detalhada

### Modo verbose (mostra cada teste)

```bash
mvn test -X
```

### Apenas testes que falharem (reexecução)

```bash
mvn test -Dsurefire.rerunFailingTestsCount=3
```

### Testes em paralelo (mais rápido)

```bash
mvn test -DparallelCount=4
```

### Ver relatório de testes após execução

```bash
mvn surefire-report:report
# Abre em target/site/surefire-report.html
```

---

## Regras de negócio implementadas

- **30 minutos grátis**; acima disso cobra por **hora cheia** (arredonda para
  cima), inclusive a primeira hora, sobre o `basePrice` do setor.
- **Preço dinâmico na entrada** conforme ocupação:
  - `< 25%` → −10% (fator 0.90)
  - `< 50%` → 0% (fator 1.00)
  - `< 75%` → +10% (fator 1.10)
  - `<= 100%` → +25% (fator 1.25)
- **Lotação 100% do estacionamento** → novas entradas (ENTRY) recusadas até liberar vaga (HTTP 409).
- **Lotação 100% do setor** → setor fechado para estacionamento (PARKED) (HTTP 409).
- Entrada (ENTRY) cria a sessão; estacionar (PARKED) marca vaga como ocupada e associa o setor; saída (EXIT) libera a vaga e calcula o valor.
- **Saída sem estacionar** → se um veículo entra e sai sem registrar o evento PARKED, a sessão é encerrada com tarifa zero (`0.00`).
