# Resumo de Implementação do Projeto Estapar Parking

Este documento descreve o que foi implementado no projeto backend de estacionamento.

## 1. Objetivo

O sistema gerencia o ciclo de estacionamento de veículos com as seguintes responsabilidades:
- inicializar a configuração da garagem a partir do simulador `/garage`;
- processar eventos de `ENTRY`, `PARKED` e `EXIT` via webhook;
- controlar vagas, setores e ocupação;
- calcular receita diária por setor;
- persistir dados em banco de dados relacional.

## 2. Stack utilizada

- Java 21
- Spring Boot 3.3
- Spring Web
- Spring Data JPA
- Flyway
- MySQL (produção)
- H2 (testes)
- Maven

## 3. Arquitetura do código

O projeto segue uma separação em camadas:
- `client/`: cliente REST para o simulador e DTOs de resposta do simulador;
- `bootstrap/`: serviço de bootstrap que carrega setores e vagas ao iniciar a aplicação;
- `config/`: configuração e propriedades do simulador;
- `domain/`: entidades JPA (`Sector`, `Spot`, `ParkingSession`, `SessionStatus`);
- `repository/`: repositórios Spring Data JPA;
- `service/`: regras de negócio e cálculos (`GarageBootstrapService`, `ParkingService`, `PricingService`, `RevenueService`);
- `web/`: controllers REST (`WebhookController`, `RevenueController`), DTOs de entrada/saída e tratamento de exceções.

## 4. Fluxo principal implementado

### 4.1 Bootstrap da garagem

- Ao subir, o `GarageBootstrapRunner` dispara `GarageBootstrapService.loadGarage()`.
- O serviço consulta o simulador em `GET /garage` usando `GarageSimulatorClient`.
- Se o banco ainda estiver vazio, são criados:
  - setores (`Sector`) com `name`, `basePrice` e `maxCapacity`;
  - vagas (`Spot`) com `externalId`, coordenadas `lat/lng`, setor associado e estado `occupied`.
- A operação é idempotente: se já existir setor cadastrado, a carga não é repetida.

### 4.2 Webhook de eventos

- Endpoint `POST /webhook` recebe eventos JSON de tipo `ENTRY`, `PARKED` e `EXIT`.
- A payload é desserializada como `WebhookEvent` polymórfico e encaminhada ao `ParkingService`.
- Todos os eventos respondem com HTTP 200 quando válidos.

### 4.3 Regras de negócio do estacionamento

#### ENTRY

- Verifica se já existe uma sessão aberta para a placa; se existir, retorna erro de negócio.
- Calcula o fator dinâmico de preço baseado na ocupação atual:
  - < 25% ocupação → desconto de 10% (`0.90`);
  - 25%–50% ocupação → preço normal (`1.00`);
  - 50%–75% ocupação → aumento de 10% (`1.10`);
  - ≥ 75% ocupação → aumento de 25% (`1.25`).
- Se todas as vagas do estacionamento inteiro estiverem ocupadas, lança `GarageFullException`.
- Persiste uma nova sessão de estacionamento com status `ENTERED` e o fator de preço definido no momento da entrada.

#### PARKED

- Localiza sessão aberta para a placa; se não existir, lança erro de negócio.
- Encontra a vaga pela latitude/longitude informada.
- Verifica se a vaga existe e se não está ocupada.
- Verifica lotação do setor da vaga; se o setor estiver 100% ocupado, rejeita com `GarageFullException`.
- Marca a vaga como ocupada, associa a vaga e o setor à sessão, define `parkedTime` e atualiza para status `PARKED`.

#### EXIT

- Localiza sessão aberta para a placa; se não existir, lança erro de negócio.
- Calcula o valor a cobrar usando `PricingService.calculateAmount(...)`:
  - primeiros 30 minutos grátis;
  - após 30 minutos, cobra por hora cheia, com arredondamento para cima;
  - usa `basePrice` do setor e o multiplicador dinâmico de entrada.
  - caso o veículo saia sem registrar o evento de estacionamento (`PARKED`), a sessão é encerrada com tarifa zero (`0.00`).
- Libera a vaga ocupada (`occupied = false`).
- Atualiza a sessão com `exitTime`, `amountCharged` e status `EXITED`.

## 5. Endpoints expostos

- `POST /webhook`
  - recebe `ENTRY`, `PARKED` e `EXIT`.
- `GET /revenue?date=YYYY-MM-DD&sector=A`
  - retorna receita total do setor para a data informada.
- `GET /`
  - endpoint de status simples.

## 6. Cálculo de faturamento

- O `RevenueService` consulta o setor pelo nome.
- Soma os valores (`amountCharged`) das sessões `EXITED` no período do dia UTC solicitado.
- Retorna `RevenueResponse` com `amount`, `currency=BRL` e `timestamp` UTC.

## 7. Tratamento de erros

- Exceções de negócio são tratadas por `GlobalExceptionHandler`.
- Há exceções específicas como `GarageFullException` e `BusinessException`.
- O serviço valida:
  - duplicidade de sessão aberta;
  - loteamento total do estacionamento;
  - lotação do setor no momento do `PARKED`;
  - inexistência de vaga ou sessão aberta.

## 8. Banco de dados e persistência

- Entidades principais:
  - `Sector`: nome, preço base, capacidade máxima;
  - `Spot`: vaga com setor, coordenadas e flag de ocupação;
  - `ParkingSession`: ciclo do veículo, incluindo entrada, estacionamento, saída, setor, vaga e valor cobrado.
- Migrations Flyway mantêm o schema inicial em `src/main/resources/db/migration/V1__init_schema.sql`.
- Configuração padrão usa MySQL em `localhost:3306`.

## 9. Testes implementados

A suíte de testes cobre unidades e integrações:
- `GarageBootstrapServiceTest`
- `PricingServiceTest`
- `ParkingServiceUnitTest`
- `RevenueServiceTest`
- `WebhookControllerTest`
- `ParkingFlowIntegrationTest`

Os testes verificam:
- carga inicial da garagem;
- cálculo de fator dinâmico e valor de cobrança;
- regras de entrada, estacionamento e saída;
- rejeição de entradas duplicadas e setor lotado;
- cálculo de receita por setor/data;
- comportamento do controller do webhook.

## 10. Observações importantes

- O valor do fator dinâmico é calculado no momento da entrada e aplicado ao cálculo final da saída.
- O sistema considera `exitTime` e `entryTime` em timestamps UTC.
- Nos testes, a aplicação roda com H2 em memória e não depende do simulador externo.
- O endpoint `/revenue` só soma sessões já finalizadas com saída registrada.
