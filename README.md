# Sistema da Escola de Artes Marciais Alessandro Silva

Painel administrativo (alunos e mensalidades) em Spring Boot 4 + HTML/CSS/JS, baseado no protótipo do Figma.

## Executar com PostgreSQL

0. O `application.properties` está no `.gitignore`. Num clone novo, copie
   `src/main/resources/application.properties.example` para `application.properties`.
1. Crie o banco: `CREATE DATABASE ace;`
2. Defina as credenciais (ou edite `src/main/resources/application.properties`):

```powershell
$env:DB_USER="postgres"; $env:DB_PASSWORD="sua-senha"
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
| `ADMIN_PASSWORD` | alessandro123              |

**Troque a senha padrão antes de colocar em produção.**

## Telas

- `/login.html` — login (Spring Security, sessão + CSRF)
- `/#/dashboard` — visão geral do mês
- `/#/alunos` — lista, busca, filtros, cadastro/edição/exclusão
- `/#/financeiro` — mensalidades do mês: registrar comprovante, confirmar, desfazer

As mensalidades do mês atual são geradas automaticamente para os alunos ativos
(`POST /api/pagamentos/gerar?mes=&ano=`). Para outros meses há um botão "Gerar mensalidades".
