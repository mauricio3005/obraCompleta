# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

`obraCompleta` (FinancialControl) is a Spring Boot backend for financial control of construction sites (obras): multi-company, multi-obra, multi-user. **`readme.md` is the authoritative domain/business spec** — every entity, enum, and business rule in the model must trace back to it. When a modeling decision changes or clarifies the domain (e.g. how a relationship works), update `readme.md` in the same change so the doc and the code never diverge.

## Commands

Use the Maven wrapper (`./mvnw` on bash, `./mvnw.cmd` on plain Windows shells) — no local Maven install required. The build works offline against the local `.m2` repo (`./mvnw -o ...`).

```bash
./mvnw compile                                  # compile main sources
./mvnw test                                     # run all tests
./mvnw test -Dtest=ClassName                    # run a single test class
./mvnw test -Dtest=ClassName#methodName          # run a single test method
./mvnw spring-boot:run                          # run the application
```

Java version: 26. Spring Boot version: 4.1.0 — note starter artifact names differ from older Spring Boot conventions (e.g. `spring-boot-starter-webmvc` not `spring-boot-starter-web`, `spring-boot-h2console` as its own starter). Check `pom.xml` before assuming an artifact id.

## Architecture

This is currently in the **pure JPA entity modeling phase**: only `model/` exists (no repositories, services, controllers, or security config yet). Entities are being built by working through `readme.md` section by section, in order.

- `src/main/java/com/mauricio/obracompleta/model/` — JPA entities (one class per entity in the readme).
- `src/main/java/com/mauricio/obracompleta/model/enums/` — all enums, kept in their own subpackage rather than nested in entities. Each enum is a closed vocabulary copied 1:1 from a `readme.md` value list (e.g. `TipoCliente`, `StatusObra`). Don't add values that aren't in the doc without updating the doc first.

### Entity conventions established so far

- Lombok on every entity: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`. Deliberately **not** `@Data` (avoids whole-field `equals`/`hashCode` on JPA proxies).
- Bean Validation (`jakarta.validation`) directly on entity fields: `@NotBlank`/`@NotNull` for required fields, `@Email`, and Hibernate Validator's Brazilian-document annotations `org.hibernate.validator.constraints.br.{CPF,CNPJ}` wherever the readme calls for a CPF/CNPJ.
- `@Id` uses `GenerationType.IDENTITY`.
- Money fields are `BigDecimal` with `@Column(precision = 15, scale = 2)`.
- All `@ManyToOne`/`@OneToOne` associations are `fetch = FetchType.LAZY`.
- **Conditionally-required fields** (a field that's only mandatory under some other field's value) are enforced with a private `@AssertTrue`-annotated boolean method on the entity, not with runtime `if` checks in a service — e.g. `Cliente` requires CPF or CNPJ depending on `tipo`; `ContaBancaria` requires `diaFechamento`/`diaVencimento` only when `tipo == CARTAO_CREDITO`; `Despesa` requires `motivoRejeicao` only when `status == REJEITADA`.
- **Polymorphic "titular"/"pagador" associations** (a field that can point at one of several unrelated entity types — Pessoa, Cliente, EmpresaPropria, Fornecedor) are modeled as an **exclusive arc**: an enum discriminator (e.g. `TipoTitular`, `TipoPagador`) plus one nullable `@ManyToOne` per possible target, with a private `@AssertTrue` method enforcing that exactly one FK is populated and it matches the discriminator. See `ContaBancaria` (titular), `Recebimento` (pagador), `Despesa`/`DespesaRecorrente` (obra+etapa vs. empresaPropria).
- Enum fields that must never change after creation (e.g. `Despesa.origem`) are additionally mapped `@Column(updatable = false)` so immutability is enforced at the JPA/DB level, not just by convention.
- Self-referencing FKs (e.g. `ContaBancaria.contaPai` for sub-contas) use a plain nullable `@ManyToOne` back to the same entity.
- File attachments are plain `String` URL/bucket-key fields on the owning entity (e.g. `Cliente.logoUrl`, `Despesa.comprovanteUrl`) — simple bucket storage, **no** signed/expiring URLs and no separate polymorphic `Anexo` entity (explicit decision, overriding an earlier draft of readme section 11).
- **Soft delete** (readme section 10/14) uses Hibernate 7's native `@SoftDelete` annotation on the `RegistroFinanceiro` `@MappedSuperclass` (`columnName = "excluido"`) — Hibernate transparently turns deletes into updates and filters queries, no `@Where`/`@SQLRestriction` boilerplate needed. `RegistroFinanceiro` also carries `excluidoEm` (a plain timestamp field the service layer must set explicitly before deleting, since `@SoftDelete` itself only manages the boolean column). Applied to `Despesa`, `DespesaRecorrente`, `Recebimento`, `Contrato`, `Aditivo`, `FolhaPagamento`, `Transferencia`, `PrestacaoContas` — the "registros financeiros" the readme calls out. Deliberately **not** applied to `LinhaFolhaPagamento` (only ever mutated while its parent folha is in `RASCUNHO`) or to cadastro/config entities (`Cliente`, `Obra`, `EtapaObra`, etc.).
- `RegistroAuditoria` is fully immutable, not soft-deletable: every column is `@Column(updatable = false)` and it must never get a delete or update service/endpoint, even for ADMIN (readme section 10 is explicit that audit records can't be edited or erased by anyone).
- `Usuario` (login/role) is a separate entity from `Funcionario` — `Usuario.funcionario` is only populated for `role == GESTOR_DE_CAIXA` (that's what ties a login to the obras it's designated to via `DesignacaoGestorObra`); `ADMIN` users have no `Funcionario` record.

### Known deliberate gaps

- Multi-tenant isolation by `EmpresaPropria` (readme section 14) is documented but not yet implemented anywhere in the model.
