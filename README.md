# Sistema da Escola de Artes Marciais Alessandro Silva

Painel administrativo (alunos e mensalidades) em Spring Boot 4 + HTML/CSS/JS, baseado no protótipo do Figma.

## Executar com PostgreSQL

0. O `application.properties` versionado lê tudo de variáveis de ambiente. Para usar
   valores locais, veja o modelo `src/main/resources/application.properties.example`.
1. Crie o banco: `CREATE DATABASE ace;`
2. Defina as credenciais (ou edite `src/main/resources/application.properties`):

```powershell
$env:DB_USER="postgres"; $env:DB_PASSWORD="sua-senha"; $env:ADMIN_PASSWORD="uma-senha-forte"
```

3. Suba a aplicação e acesse http://localhost:8080:

```powershell
./mvnw spring-boot:run
```

As tabelas são criadas automaticamente (`ddl-auto=update`).

## Executar sem banco (H2 em memória, com alunos de exemplo)

```powershell
./mvnw spring-boot:test-run
```

Os dados somem ao reiniciar.

## Acesso

| Variável         | Padrão                     |
|------------------|----------------------------|
| `ADMIN_EMAIL`    | admin@alessandrosilva.com  |
| `ADMIN_PASSWORD` | **obrigatória**, mínimo de 10 caracteres (sem padrão) |
| `APP_TIMEZONE`   | America/Sao_Paulo (via `app.timezone`) |

O sistema não sobe sem `ADMIN_PASSWORD`. Depois de 5 senhas erradas, o login fica
bloqueado por 15 minutos para aquele IP (20 erradas bloqueiam a conta pelo mesmo tempo).

No perfil H2 (`spring-boot:test-run`) a senha de desenvolvimento fica em
`src/test/resources/application-h2.properties`.

## Telas

O painel funciona no celular (barra de navegação inferior, listas compactas) e no
computador (menu lateral, tabelas). A partir de 1280px de largura entra o layout de computador; abaixo disso (inclusive tablets) ficam o menu inferior e os cartões.

- `/login.html` — login (Spring Security, sessão + CSRF)
- `/#/` — início: indicadores do mês, comprovantes para conferir, mensalidades em atraso e receita recente
- `/#/alunos` — lista com busca, filtros (status, modalidade), ordenação e exportação CSV
- `/#/alunos/{id}` — ficha do aluno: dados, WhatsApp, histórico de mensalidades e graduações
- `/#/alunos/novo` e `/#/alunos/{id}/editar` — cadastro e edição
- `/#/financeiro` — mensalidades do mês: registrar/desfazer comprovante, confirmar, desfazer confirmação, cobrar por WhatsApp
- `/#/relatorios` — receita dos últimos 6 ou 12 meses, alunos por modalidade e exportações CSV

## Regras das mensalidades

- As mensalidades do mês atual são geradas automaticamente para os alunos ativos
  (`POST /api/pagamentos/gerar?mes=&ano=`). Para outros meses há um botão "Gerar mensalidades".
- Meses anteriores à data de matrícula do aluno não são cobrados.
- Cada aluno tem um dia de vencimento (1 a 28, padrão 10). Mensalidade pendente depois do
  vencimento aparece como **atrasada**, com os dias de atraso.
- Mudanças de valor ou de dia de vencimento valem só para as mensalidades ainda pendentes.
- Um comprovante registrado por engano pode voltar para pendente.
- Registrar uma graduação atualiza a faixa atual do aluno (vale a graduação mais recente).

## Código do frontend

Sem build: módulos JavaScript nativos em `src/main/resources/static/js`.

- `common.js` — ícones, formatação de dinheiro e chamadas à API (compartilhado com o login)
- `app/main.js` — rotas, carregamento de dados e ações
- `app/state.js` — estado e cálculos do mês
- `app/ui.js` — componentes compartilhados (menu, diálogos, avisos, botões de pagamento)
- `app/format.js` — datas, telefone, WhatsApp e CSV
- `app/views/*.js` — uma tela por arquivo
