# Agenda de Shows

Aplicação web para gerenciar a agenda de shows de um músico/banda: cadastro, edição,
exclusão, controle de cachê pago e filtros por período, artista, local e situação de pagamento.

Desenvolvida com **Java 21**, **Spring Boot 4**, **Thymeleaf**, **Spring Security**, **Spring Data JPA**
e **MySQL**, com migrações de banco via **Flyway**.

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Stack tecnológica](#stack-tecnológica)
- [Arquitetura](#arquitetura)
- [Modelo de dados](#modelo-de-dados)
- [Rotas da aplicação](#rotas-da-aplicação)
- [Configuração](#configuração)
- [Como executar](#como-executar)
- [Testes](#testes)
- [Dados de exemplo](#dados-de-exemplo)
- [Estrutura do projeto](#estrutura-do-projeto)

---

## Funcionalidades

- **Autenticação** com login por formulário e senha criptografada com BCrypt.
- **Proteção de rotas**: todas as páginas exigem autenticação, exceto login, assets e página de erro.
- **CRUD de shows** com data, artista, local, horário, observações, cachê e status de pagamento.
- **Cadastro em lote**: gera um show para cada ocorrência dos dias da semana marcados no mês/ano
  selecionados, copiando horário, cachê, artista, local, observações e pagamento mensal.
- **Listagem paginada** (20 itens por página), ordenada por data crescente.
- **Filtros combináveis**:
  - mês e/ou ano (comportamento diferente quando apenas um dos dois é informado);
  - situação de pagamento (pago / não pago);
  - artista, local e observações (busca parcial, sem diferenciar maiúsculas/minúsculas);
  - opção "somente a partir do mês atual" (marcada por padrão).
- **Filtro automático** no navegador: selects, números e checkbox disparam a busca ao mudar; campos de
  texto aguardam 600 ms após a digitação.
- **Marcação rápida de pagamento** direto na listagem, preservando os filtros aplicados.
- **Interface responsiva** com Bootstrap 5 (tabela adaptada para telas pequenas com menu de ações).
- **Validação de formulário** com mensagens em português.
- **Usuário inicial** criado automaticamente na primeira execução, quando o banco está vazio.
- **Flash messages** para sucesso e erro das operações.

---

## Stack tecnológica

| Item | Versão / Tecnologia |
| --- | --- |
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.2 |
| Persistência | Spring Data JPA / Hibernate |
| Banco | MySQL 8.4 (produção/desenvolvimento) |
| Migrações | Flyway |
| Segurança | Spring Security 6 + Thymeleaf Extras |
| Templates | Thymeleaf |
| Front-end | Bootstrap 5.3 (CDN) |
| Build | Gradle 9.9.1 (wrapper) |
| Testes | JUnit 5, MockMvc, H2 em modo MySQL |
| Utilitários | Lombok |

---

## Arquitetura

O projeto segue uma organização por domínio (*package by feature*), com camadas bem definidas:

```
com.example.agendashows
├── config          # Configuração de segurança
├── show
│   ├── controller  # Endpoints MVC (Thymeleaf)
│   ├── dto         # ShowForm (entrada) e ShowFilter (filtros)
│   ├── entity      # Show + enum StatusPagamento
│   ├── exception   # ShowNotFoundException
│   ├── repository  # Spring Data JPA
│   ├── service     # Regras de negócio e transações
│   └── specification # Consultas dinâmicas com Criteria API
└── user
    ├── controller  # Login e redirecionamento da raiz
    ├── entity      # AppUser
    ├── repository  # AppUserRepository
    └── service     # UserDetailsService + criação do usuário inicial
```

Fluxo resumido de uma requisição:

```
Navegador → SecurityFilterChain → ShowController → ShowService → ShowRepository
                                                                ↓
                                                       ShowSpecifications
                                                                ↓
                                                    Template Thymeleaf (shows/list)
```

---

## Modelo de dados

As tabelas são criadas pelo Flyway (`src/main/resources/db/migration`), e o Hibernate está configurado
com `ddl-auto=validate`, ou seja, apenas valida o esquema.

### `shows`

| Coluna | Tipo | Regras |
| --- | --- | --- |
| `id` | `BIGINT` | PK, auto incremento |
| `data` | `DATE` | obrigatório |
| `artista` | `VARCHAR(150)` | obrigatório |
| `local` | `VARCHAR(200)` | obrigatório |
| `horario` | `TIME` | opcional |
| `observacoes` | `VARCHAR(2000)` | opcional |
| `cache` | `DECIMAL(10,2)` | opcional, não negativo |
| `pago` | `BOOLEAN` | obrigatório, padrão `false` |

Índice: `idx_shows_data` em `data`.

### `usuario`

| Coluna | Tipo | Regras |
| --- | --- | --- |
| `id` | `BIGINT` | PK, auto incremento |
| `nome_usuario` | `VARCHAR(50)` | obrigatório, único |
| `senha` | `VARCHAR(100)` | obrigatório, hash BCrypt |
| `ativo` | `BOOLEAN` | obrigatório, padrão `true` |

### Regras dos filtros de data

- **Ano + mês**: do primeiro dia do mês até o primeiro dia do mês seguinte (intervalo exclusivo no fim).
- **Somente ano**: do primeiro dia do ano até o primeiro dia do ano seguinte.
- **Somente mês**: considera aquele mês em qualquer ano (função `MONTH` do banco).
- **"Somente a partir do mês atual"**: aplica `data >= primeiro dia do mês corrente`.
- Os filtros de texto usam `LIKE` com `%...%`, em minúsculas e com escape de `%`, `_` e `\`.

---

## Rotas da aplicação

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/` | Redireciona para `/estatisticas` |
| `GET` | `/entrar` | Página de login |
| `POST` | `/entrar` | Processa o login |
| `POST` | `/sair` | Logout |
| `GET` | `/shows` | Listagem paginada com filtros |
| `GET` | `/shows/novo` | Formulário de cadastro |
| `POST` | `/shows` | Cria um show |
| `GET` | `/shows/lote` | Formulário de cadastro em lote |
| `POST` | `/shows/lote` | Cria um show para cada ocorrência dos dias da semana selecionados no mês/ano |
| `GET` | `/shows/{id}/editar` | Formulário de edição |
| `POST` | `/shows/{id}` | Atualiza um show |
| `POST` | `/shows/{id}/pagamento` | Alterna pago / não pago |
| `POST` | `/shows/{id}/excluir` | Exclui um show |

> As rotas `POST` exigem token CSRF, conforme o padrão do Spring Security.

### Parâmetros de filtro (`GET /shows`)

| Parâmetro | Descrição |
| --- | --- |
| `mes` | Mês (1–12) |
| `ano` | Ano (1–9999) |
| `somenteFuturos` | `true` (padrão) para mostrar a partir do mês atual |
| `situacaoPagamento` | `PAGO` ou `NAO_PAGO` |
| `artista` | Trecho do nome do artista |
| `local` | Trecho do local |
| `observacoes` | Trecho das observações |
| `page` | Número da página (base 0) |
| `size` | Tamanho da página (padrão 20) |

---

## Configuração

Arquivos em `src/main/resources`:

- `application.properties` — configurações comuns (perfil `dev` ativo por padrão, locale `pt_BR`,
  `ddl-auto=validate`, `open-in-view=false`).
- `application-dev.properties` — MySQL local e usuário inicial padrão.
- `application-prod.properties` — dados sensíveis via variáveis de ambiente.
- `src/test/resources/application-test.properties` — H2 em memória no modo MySQL.

### Variáveis de ambiente

| Variável | Padrão (dev) | Descrição |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/agendashows` | URL do banco (obrigatória em produção) |
| `DB_USUARIO` | `root` (dev) | Usuário do banco (obrigatória em produção) |
| `DB_SENHA` | `123456` (dev) | Senha do banco (obrigatória em produção) |
| `USUARIO_INICIAL` | `admin` (dev) | Nome do usuário criado no primeiro start |
| `SENHA_INICIAL` | `admin` (dev) | Senha do usuário inicial (armazenada como BCrypt) |
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil ativo (`dev` ou `prod`) |

> O usuário inicial só é criado quando a tabela `usuario` está vazia. Defina `USUARIO_INICIAL` e
> `SENHA_INICIAL` em produção e altere a senha após o primeiro acesso.

---

## Como executar

### Pré-requisitos

- JDK 21
- Docker (opcional, para o MySQL) ou uma instância MySQL 8 local
- Não é necessário instalar Gradle: use o wrapper (`./gradlew`)

### 1. Subir o banco com Docker

```bash
docker compose up -d
```

O `docker-compose.yml` cria o banco `agendashows` no MySQL 8.4, com usuário `root` e senha `123456`,
expondo a porta `3306`.

### 2. Executar a aplicação

```bash
./gradlew bootRun
```

A aplicação sobe em `http://localhost:8080`. O Flyway cria o esquema automaticamente e o usuário
inicial `admin` / `admin` é criado se o banco estiver vazio.

### 3. Build e execução do JAR

```bash
./gradlew build
java -jar build/libs/agendashows-0.0.1-SNAPSHOT.jar
```

### Executar com o perfil de produção

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL="jdbc:mysql://host:3306/agendashows"
export DB_USUARIO="usuario"
export DB_SENHA="senha"
export USUARIO_INICIAL="admin"
export SENHA_INICIAL="uma-senha-forte"
java -jar build/libs/agendashows-0.0.1-SNAPSHOT.jar
```

---

## Testes

Os testes usam H2 em memória no modo MySQL e cobrem repositório (filtros/paginação), controller
(rotas, validações, CSRF, filtros) e segurança.

```bash
./gradlew test
```

Relatório gerado em `build/reports/tests/test/index.html`.

---

## Dados de exemplo

O arquivo `sql/shows_inserts.sql` contém um dump com inserts de exemplo da tabela `shows`. Para
carregá-los no banco local:

```bash
docker exec -i mysql mysql -uroot -p123456 agendashows < sql/shows_inserts.sql
```

---

## Estrutura do projeto

```
agendashows/
├── build.gradle
├── docker-compose.yml          # MySQL 8.4 para desenvolvimento
├── gradlew / gradlew.bat       # Gradle Wrapper
├── sql/
│   └── shows_inserts.sql       # Dados de exemplo
└── src/
    ├── main/
    │   ├── java/com/example/agendashows/
    │   │   ├── AgendashowsApplication.java
    │   │   ├── config/SecurityConfig.java
    │   │   ├── show/...
    │   │   └── user/...
    │   └── resources/
    │       ├── application*.properties
    │       ├── db/migration/   # V1 (usuario) e V2 (shows)
    │       └── templates/      # login, listagem, formulário e fragmentos
    └── test/
        ├── java/...            # testes de integração
        └── resources/application-test.properties
```
