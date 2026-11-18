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
- [Relatório semanal por e-mail](#relatório-semanal-por-e-mail)
- [Calendário (Apple Calendar)](#calendário-apple-calendar)
- [Deploy em produção](#deploy-em-produção)
- [Estrutura do projeto](#estrutura-do-projeto)

---

## Funcionalidades

- **Autenticação** com login por formulário e senha criptografada com BCrypt.
- **Proteção de rotas**: todas as páginas exigem autenticação, exceto login, assets, página de erro e
  o feed do calendário (protegido por token na URL).
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
- **Relatório semanal por e-mail**: toda segunda-feira às 8h (fuso `America/Sao_Paulo`) a aplicação envia
  para o usuário principal o relatório com os shows da semana (segunda a domingo), via SMTP ou API do Brevo.
- **Calendário no Apple Calendar**: feed iCalendar com os próximos shows, disponível para assinatura
  (atualiza sozinho) ou download avulso.
- **Flash messages** para sucesso e erro das operações.

---

## Stack tecnológica

| Item | Versão / Tecnologia |
| --- | --- |
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.2 |
| Persistência | Spring Data JPA / Hibernate |
| Banco | MySQL 8.4 (desenvolvimento e produção) |
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
| `email` | `VARCHAR(254)` | opcional; destinatário do relatório semanal |
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
| `GET` | `/` | Redireciona para `/dashboard` |
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
| `GET` | `/calendario/{token}.ics` | Feed iCalendar dos próximos shows (público, protegido pelo token) |

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
| `EMAIL_INICIAL` | vazio | E-mail do usuário principal; também preenche o usuário existente se estiver sem e-mail |
| `EMAIL_TRANSPORTE` | `smtp` | `smtp` (VPS/dev) ou `brevo` (API HTTPS, para Railway Free/Trial/Hobby) |
| `BREVO_API_KEY` | vazio | Chave da API do Brevo; obrigatória no transporte `brevo` |
| `BREVO_API_URL` | `https://api.brevo.com/v3/smtp/email` | Endpoint da API do Brevo |
| `MAIL_REMETENTE` | `MAIL_USUARIO` | Remetente do e-mail do relatório (verificado no provedor) |
| `MAIL_NOME_REMETENTE` | `Agenda de Shows` | Nome exibido como remetente |
| `MAIL_HOST` | vazio | Servidor SMTP; usado apenas no transporte `smtp` |
| `MAIL_PORT` | `587` | Porta SMTP |
| `MAIL_USUARIO` | vazio | Usuário de autenticação SMTP |
| `MAIL_SENHA` | vazio | Senha (de app) do SMTP |
| `MAIL_REMETENTE` | `MAIL_USUARIO` | Remetente do e-mail do relatório |
| `MAIL_AUTH` | `true` | Habilita autenticação SMTP |
| `MAIL_STARTTLS` | `true` | Habilita STARTTLS |
| `EMAIL_RELATORIO_CRON` | `0 0 8 * * MON` | Cron do envio (segunda, 8h) |
| `APP_FUSO` | `America/Sao_Paulo` | Fuso usado no agendamento, no período do relatório e no calendário |
| `CALENDARIO_TOKEN` | vazio | Token secreto do feed `/calendario/{token}.ics`; vazio desabilita o feed |
| `CALENDARIO_DURACAO_MINUTOS` | `180` | Duração dos eventos com horário no feed do calendário |
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil ativo (`dev` ou `prod`) |
| `PORT` | `8080` | Porta HTTP (usada por PaaS que injetam a porta) |
| `DB_POOL_MAX` | `5` | Tamanho máximo do pool de conexões em produção |
| `COOKIE_SECURE` | `false` | `true` quando a aplicação é servida somente por HTTPS |

> O usuário inicial só é criado quando a tabela `usuario` está vazia. Defina `USUARIO_INICIAL` e
> `SENHA_INICIAL` em produção e altere a senha após o primeiro acesso.
>
> O `docker-compose.prod.yml` usa ainda `APP_DOMAIN`, `DB_NOME` e `DB_ROOT_SENHA`, descritas na
> seção [Deploy em produção](#deploy-em-produção).

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

## Relatório semanal por e-mail

O agendador roda dentro da própria aplicação, sem serviço externo de cron: toda **segunda-feira às 8h**
(horário de `APP_FUSO`, padrão `America/Sao_Paulo`) ele monta e envia um e-mail com os shows da semana,
de **segunda a domingo** (as datas da segunda do envio até o domingo seguinte, inclusive).

- O destinatário é o **usuário principal**: o primeiro usuário ativo, em ordem de cadastro, com `email`
  preenchido na tabela `usuario`.
- O e-mail lista data, horário, artista, local, cachê, situação do pagamento e observações, além do
  total de shows, cachê total, pagos e pendentes.
- Sem shows no período, o e-mail é enviado normalmente com "Nenhum show agendado para esta semana".
- Se não houver usuário com e-mail, o envio é apenas registrado em log (`WARN`), sem afetar a aplicação.
- O envio usa **SMTP ou a API HTTPS do Brevo**, escolhido por `EMAIL_TRANSPORTE`. Sem o transporte
  configurado, o agendador não é criado (a aplicação sobe normalmente).

### Transportes

| `EMAIL_TRANSPORTE` | Requer | Quando usar |
| --- | --- | --- |
| `smtp` (padrão) | `MAIL_HOST` e credenciais `MAIL_*` | VPS com Docker Compose e desenvolvimento |
| `brevo` | `BREVO_API_KEY` e `MAIL_REMETENTE` verificado | Railway Free/Trial/Hobby, que bloqueiam SMTP de saída |

> A Railway bloqueia as portas SMTP 25, 465, 587 e 2525 nos planos Free, Trial e Hobby; somente o
> plano Pro libera. A API do Brevo usa HTTPS (porta 443), que funciona em qualquer plano.

### Configuração com Brevo (Railway)

1. Crie uma conta gratuita no [Brevo](https://www.brevo.com) (plano Free: ~300 e-mails/dia).
2. Em **Senders, Domains & IPs → Senders**, cadastre e confirme o e-mail remetente (não é preciso
   domínio próprio; basta clicar no link de confirmação).
3. Em **SMTP & API → API Keys**, crie uma chave e use como `BREVO_API_KEY`.
4. Configure as variáveis:

```dotenv
EMAIL_INICIAL=seu-email@exemplo.com
EMAIL_TRANSPORTE=brevo
BREVO_API_KEY=xkeysib-...
MAIL_REMETENTE=seu-email@exemplo.com   # o mesmo sender confirmado no Brevo
MAIL_NOME_REMETENTE=Agenda de Shows
```

No start, o log confirma o transporte:
`Relatório semanal habilitado: transporte brevo, remetente '...', cron '0 0 8 * * MON', fuso 'America/Sao_Paulo'.`
Erros da API aparecem como `Brevo respondeu HTTP 400/401 ...` (sender não verificado, chave inválida
ou limite diário), sem derrubar a aplicação.

### Configuração com SMTP (VPS/desenvolvimento)

```dotenv
EMAIL_INICIAL=seu-email@exemplo.com

EMAIL_TRANSPORTE=smtp
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USUARIO=seu-email@exemplo.com
MAIL_SENHA=sua-senha-de-app
MAIL_REMETENTE=seu-email@exemplo.com
```

> No Gmail, ative a verificação em duas etapas e gere uma **senha de app**; a senha normal da conta não
> funciona via SMTP. Para outros provedores, use host/porta indicados por eles (normalmente 587 com
> STARTTLS). Para um servidor SMTP local sem autenticação, use `MAIL_AUTH=false` e `MAIL_STARTTLS=false`.

- O e-mail do usuário existente pode ser preenchido via `EMAIL_INICIAL` no próximo start ou diretamente
  no banco: `UPDATE usuario SET email = '...' WHERE nome_usuario = 'admin';`
- Para desativar o envio, remova `BREVO_API_KEY` (transporte `brevo`) ou `MAIL_HOST` (transporte `smtp`).
- Para testar o horário, ajuste `EMAIL_RELATORIO_CRON` (sintaxe de cron do Spring, 6 campos) e reinicie.
- Como o agendador fica na aplicação, mantenha **uma única instância** rodando (a sessão de login também
  é em memória).

---

## Calendário (Apple Calendar)

A listagem de shows tem um menu **Calendário** que permite assinar o feed iCalendar (`/calendario/{token}.ics`)
no Apple Calendar. A assinatura é atualizada automaticamente pelo próprio app: shows novos ou editados
aparecem sem precisar exportar de novo.

- O feed traz apenas os shows **a partir de hoje**, em ordem cronológica.
- Shows com horário viram eventos de **3 horas** (`CALENDARIO_DURACAO_MINUTOS` para mudar), com fuso
  `APP_FUSO`; shows sem horário viram eventos de **dia inteiro**.
- O `SUMMARY` é o artista, o `LOCATION` é o local e a `DESCRIPTION` são as observações.

### Configurar

1. Gere um token e defina `CALENDARIO_TOKEN` no ambiente (ex.: `openssl rand -hex 24`). Sem token, o feed
   responde `404`.
2. Reinicie a aplicação e, na tela de shows, use **Calendário › Copiar link de assinatura**.
3. **iPhone:** Ajustes › Calendário › Contas › Adicionar conta › Outra › Assinar calendário (cole o link
   `https://.../calendario/<token>.ics`). O botão **Assinar calendário** também abre o `webcal://` direto.
4. **Mac:** Calendário › Arquivo › Nova assinatura de calendário e cole o link.

O link é um segredo: quem o tiver consegue ler os próximos shows. Para revogar um link vazado, troque o
`CALENDARIO_TOKEN` e reative a assinatura. Para baixar um arquivo avulso, o menu oferece **Baixar arquivo .ics**.

---

## Deploy em produção

### Opção 1 — Railway (plano Hobby)

O Railway hospeda a aplicação e o MySQL no mesmo projeto, sem sleep e com HTTPS automático.
O plano Hobby custa **US$ 5/mês** e inclui **US$ 5 de uso**; um app pequeno com banco costuma ficar
entre US$ 6 e US$ 10/mês.

| Recurso | Plano Hobby |
| --- | --- |
| Aplicação | sem sleep, até 48 vCPU / 48 GB por serviço |
| MySQL | template oficial `mysql:8` com volume de até 5 GB |
| Domínio | `https://<nome>.up.railway.app` com certificado automático |
| Região recomendada | US East (Virginia), a de menor latência para o Brasil |

Arquivos usados: `Dockerfile` (imagem da aplicação) e `railway.json` (build e healthcheck).

#### 1. Criar o projeto e o banco

1. Crie a conta em [railway.com](https://railway.com) entrando com o GitHub.
2. **New Project → Deploy from GitHub repo** e selecione `agendashows`.
3. No projeto, clique em **New → Database → MySQL**.
4. Aguarde o serviço MySQL ficar **Online**. O template cria automaticamente:

   | Variável | Uso |
   | --- | --- |
   | `MYSQLHOST` | host interno (`*.railway.internal`) |
   | `MYSQLPORT` | `3306` |
   | `MYSQLUSER` | `root` |
   | `MYSQLPASSWORD` | senha gerada |
   | `MYSQLDATABASE` | `railway` |

> O template pode usar MySQL 9.x. Se quiser a mesma versão do projeto, altere a imagem para
> `mysql:8.4` em **Settings → Source** antes do primeiro deploy (o volume é criado no primeiro
> start).

#### 2. Configurar a aplicação

No serviço **da aplicação** (não no MySQL), abra **Variables → Raw Editor** e cole:

```dotenv
SPRING_PROFILES_ACTIVE=prod
TZ=America/Sao_Paulo
COOKIE_SECURE=true
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8
DB_USUARIO=${{MySQL.MYSQLUSER}}
DB_SENHA=${{MySQL.MYSQLPASSWORD}}
USUARIO_INICIAL=admin
SENHA_INICIAL=<uma-senha-forte>
EMAIL_INICIAL=<seu-email@exemplo.com>
EMAIL_TRANSPORTE=brevo
BREVO_API_KEY=<chave-da-api-brevo>
MAIL_REMETENTE=<seu-email@exemplo.com>
MAIL_NOME_REMETENTE=Agenda de Shows
CALENDARIO_TOKEN=<openssl rand -hex 24>
```

> As referências `${{MySQL.*}}` são resolvidas pelo Railway. Se o serviço de banco tiver outro
> nome, ajuste o prefixo. O banco padrão chama-se `railway`; se quiser `agendashows`, altere
> `MYSQL_DATABASE` no serviço MySQL antes do primeiro start.

Depois, em **Settings → Networking → Generate Domain**, gere a URL pública.

#### 3. Deploy

O Railway reconstrói a cada push no GitHub. Acompanhe em **Deployments → Logs**: o Flyway aplica
`V1`–`V4`, o Hibernate valida o schema e o usuário inicial é criado. Acesse a URL gerada.

#### 4. Migrar os dados locais (opcional)

O app cria o schema e o usuário inicial. Para importar os shows do MySQL local, use o host e a
porta públicos do serviço MySQL (**Settings → Networking → Public Access**):

```bash
docker exec mysql mysqldump -uroot -p123456 --no-create-info --complete-insert \
  agendashows shows > shows.sql

docker run --rm -i mysql:8.4 mysql -h <host-publico> -P <porta-publica> \
  -u root -p"<senha-do-MySQL>" railway < shows.sql
```

Importe apenas `shows`; não importe `usuario` nem `flyway_schema_history`.

#### 5. Backup e operação

- Ative os backups do banco no dashboard do MySQL (**Settings → Backups**) ou faça dumps
  periódicos com `mysqldump`.
- A sessão de login fica em memória: mantenha **uma única réplica**.
- O volume do MySQL no Hobby tem até 5 GB, suficiente para esta aplicação.
- Confira o consumo em **Usage**; o Hobby cobra apenas o que ultrapassar os US$ 5 inclusos.

---

### Opção 2 — VPS com Docker Compose

Alternativa para quem tem (ou conseguir) uma VM Linux, como Oracle Cloud, Hetzner ou outra.
A stack completa (MySQL 8.4, aplicação e Caddy com HTTPS) roda em containers.

| Arquivo | Função |
| --- | --- |
| `Dockerfile` | Build multi-stage do JAR e imagem de execução com JRE 21 |
| `docker-compose.prod.yml` | MySQL, aplicação e Caddy na mesma rede interna |
| `Caddyfile` | Proxy reverso e certificado Let's Encrypt para `APP_DOMAIN` |
| `.env.example` | Modelo das variáveis usadas no deploy |
| `deploy.sh` | Atualiza o código e recria os containers |
| `backup.sh` | Dump diário do MySQL com retenção configurável |

#### 1. Preparar a VM

Crie uma instância `VM.Standard.A1.Flex` (2 OCPU / 12 GB) com Ubuntu 24.04 e IP público reservado.
Depois, no servidor:

```bash
sudo apt update && sudo apt -y upgrade
sudo apt -y install docker.io docker-compose-v2 git ufw
sudo usermod -aG docker ubuntu && newgrp docker
sudo timedatectl set-timezone America/Sao_Paulo
```

#### 2. Liberar as portas

No firewall do provedor (Security List/NSG, no caso da Oracle), libere `80/tcp`, `443/tcp` e
`443/udp` para `0.0.0.0/0` e `22/tcp` apenas para o seu IP. Na própria VM:

```bash
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 443/udp
sudo ufw enable
```

> As imagens Ubuntu da Oracle trazem regras de `iptables` que rejeitam o tráfego antes do UFW.
> Se 80/443 continuarem inacessíveis, verifique com `sudo iptables -L INPUT --line-numbers` e
> insira regras `ACCEPT` antes do `REJECT` existente (ou remova a regra de `REJECT`).

#### 3. Apontar o domínio

Crie um subdomínio gratuito no [DuckDNS](https://www.duckdns.org) (por exemplo,
`agendashows.duckdns.org`) apontando para o IP público da VM. O Caddy emite e renova o certificado
Let's Encrypt automaticamente. Alternativas: `nip.io`/`sslip.io` ou um domínio próprio.

#### 4. Configurar e subir a aplicação

```bash
git clone https://github.com/rafarodriguespiano-maker/agendashows.git ~/agendashows
cd ~/agendashows

cp .env.example .env
nano .env   # APP_DOMAIN, DB_*, USUARIO_INICIAL, SENHA_INICIAL, EMAIL_INICIAL, MAIL_* e CALENDARIO_TOKEN

chmod +x deploy.sh backup.sh
./deploy.sh
```

Para acompanhar a inicialização (Flyway, validação do schema e criação do usuário inicial):

```bash
docker compose -f docker-compose.prod.yml logs -f app
```

Depois acesse `https://<APP_DOMAIN>` e entre com `USUARIO_INICIAL` / `SENHA_INICIAL`.

> O `docker-compose.prod.yml` lê o `.env` também como `env_file` do container da aplicação, então as
> variáveis `MAIL_*` e `EMAIL_INICIAL` definidas nele são repassadas automaticamente.
>
> O MySQL só aplica `DB_USUARIO` e `DB_SENHA` na primeira inicialização do volume. Se precisar
> alterar a senha depois, use `ALTER USER` dentro do container ou recrie o volume (isso apaga os
> dados). O mesmo vale para o usuário inicial: ele só é criado quando a tabela `usuario` está vazia.

#### 5. Backup diário

```bash
mkdir -p /home/ubuntu/backups
crontab -e
```

```cron
0 3 * * * /home/ubuntu/agendashows/backup.sh >> /home/ubuntu/backups/backup.log 2>&1
```

Os dumps ficam em `~/backups/agendashows` e são mantidos por 7 dias (`RETENCAO_DIAS`). Copie-os
periodicamente para fora da VM.

#### 6. Atualizar a aplicação

```bash
cd ~/agendashows && ./deploy.sh
```

> A sessão de login fica em memória, então a aplicação deve rodar em **uma única instância**.
> O MySQL não expõe porta no host; o acesso é apenas pela rede interna do Compose.

---

## Estrutura do projeto

```
agendashows/
├── build.gradle
├── Dockerfile                  # Imagem de produção (build + JRE 21)
├── railway.json                # Build e healthcheck no Railway
├── docker-compose.yml          # MySQL 8.4 para desenvolvimento
├── docker-compose.prod.yml     # MySQL + aplicação + Caddy para produção
├── Caddyfile                   # Proxy reverso e HTTPS automático
├── .env.example                # Modelo das variáveis de deploy
├── deploy.sh / backup.sh       # Deploy e backup no servidor
├── gradlew / gradlew.bat       # Gradle Wrapper
├── sql/
│   └── shows_inserts.sql       # Dados de exemplo
└── src/
    ├── main/
    │   ├── java/com/example/agendashows/
    │   │   ├── AgendashowsApplication.java
    │   │   ├── config/         # segurança e agendamento
    │   │   ├── relatorio/      # relatório semanal por e-mail (job + templates)
    │   │   ├── show/...
    │   │   └── user/...
    │   └── resources/
    │       ├── application*.properties
    │       ├── db/migration/   # V1–V4 (usuario e shows)
    │       └── templates/      # login, listagem, formulário, fragmentos e e-mail
    └── test/
        ├── java/...            # testes de integração
        └── resources/application-test.properties
```
