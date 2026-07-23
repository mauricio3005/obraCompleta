# FinancialControl — Context

Sistema de controle financeiro para obras (construção civil). Multi-empresa, multi-obra, multi-usuário. Foco deste documento: **backend Java/Spring + modelagem JPA**. Regras de negócio e campos completos, linguagem técnica, sem prosa de venda.

## Objetivo do sistema

Centralizar em uma única plataforma o que hoje está fragmentado em planilhas: despesas, contratos, recebimentos, orçamento por etapa, folha de pagamento, contas bancárias, prestação de contas. Multiusuário com permissões por papel. Histórico de auditoria completo e imutável. O sistema deve responder a qualquer momento:
- Saldo disponível de cada obra
- Saldo de cada conta bancária
- Saúde financeira de cada empresa própria 

---

## 1. Entidades de cadastro base

### Cliente
Contratante externo (quem paga pela obra).
- nome (obrigatório, único)
- tipo: PESSOA_JURIDICA | PESSOA_FISICA | ORGAO_PUBLICO
- CNPJ (se PJ) ou CPF (se PF)
- logo (imagem, usada em relatórios/documentos)
- endereço, telefone, email

### EmpresaPropria
Razão social sob a qual o dono opera. Cada uma tem contas bancárias, funcionários e despesas administrativas próprias.
- nome (único)
- CNPJ (opcional)
- logo, endereço, telefone, email
- status: ATIVA | INATIVA

### Pessoa
Identidade física — entidade base. Uma pessoa pode acumular múltiplos papéis (funcionário de várias empresas, titular de várias contas, usuário do sistema).
- nome (obrigatório)
- CPF (único, opcional)
- telefone, email

### Fornecedor
Quem recebe pagamento em uma despesa.
- nome (único)
- categoria (material, serviço, equipamento, etc.)
- contato, CNPJ ou CPF, telefone, email

### Funcionario
Vínculo de trabalho remunerado, associado a uma `Pessoa`. `tipo`: FOLHA_BASE | GESTOR_CAIXA — define a natureza do vínculo (não é só um rótulo, muda qual caixa é debitado):
- **FOLHA_BASE**: só aparece na folha de pagamento de uma obra; não tem login nem movimenta dinheiro do sistema. Custo sempre cai como despesa **da obra** (via `VinculoObra` + `FolhaPagamento`, origem=FOLHA).
- **GESTOR_CAIXA**: tem login, recebe transferências para gerir gastos em campo, precisa prestar contas. O próprio salário/pró-labore dele é despesa **administrativa da empresa** (via `VinculoEmpresa`) — é uma pessoa da empresa, não da obra. O dinheiro que ele movimenta em campo só vira despesa da obra depois de aprovada a prestação de contas (origem=PRESTACAO_CONTAS); nunca é contabilizado como pagamento a ele mesmo.

**Vínculos de pagamento** (entidades de associação — função e regra de pagamento não são atributo direto da Pessoa/Funcionario):
- **VinculoObra** (Funcionario↔Obra): função + regra de pagamento (DIARIA | FIXO | METRO_QUADRADO) + valor. Alimenta a `FolhaPagamento`, gera despesa da obra. Uso típico do FOLHA_BASE; um GESTOR_CAIXA também pode ter (ex.: além do salário fixo pela empresa, também trabalha por diária em alguma obra específica).
- **VinculoEmpresa** (Funcionario↔EmpresaPropria): função + salário. Gera despesa administrativa da empresa. Uso típico e exclusivo na prática do GESTOR_CAIXA — FOLHA_BASE não tem esse vínculo, é 100% amarrado a obra(s).

Uma mesma pessoa pode acumular múltiplos `VinculoObra` em obras diferentes, cada um com sua própria função e regra de pagamento (ex.: mesma pessoa é "Mestre" fixo em uma obra e "Pedreiro" por diária em outra).

### DesignacaoGestorObra
Não é vínculo de pagamento, é autorização: liga um `Funcionario` do tipo GESTOR_CAIXA às obras onde ele pode transferir/prestar contas e às quais tem acesso no sistema (row-level — ver seção 12).

---

## 2. Obras e etapas

### Obra
Entidade central — quase toda movimentação financeira se liga, direta ou indiretamente, a uma obra.
- nome (único)
- cliente (FK Cliente)
- status: ATIVA | CONCLUIDA | PAUSADA | CANCELADA
- data prevista de início e fim
- valor do contrato com o cliente
- orçamento total previsto (custos)
- número do contrato, número da ART
- endereço completo
- descrição livre
- anexo: PDF do contrato assinado

### EtapaObra
Cada obra define seu próprio conjunto de etapas — **não há etapas globais/compartilhadas** entre obras.
- nome (ex.: Fundação, Estrutura, Alvenaria, Acabamento)
- ordem (inteiro, para respeitar sequência em relatórios)
- FK Obra

### OrcamentoEtapa
Valor previsto por etapa, dividido por tipo de custo:
- mão de obra, material, outros custos (permite comparar previsto x realizado depois)

### TaxaConclusaoEtapa
Percentual de execução física (0–100%), atualizável ao longo do tempo. **Manter histórico** de cada atualização (não sobrescrever — tabela de histórico com timestamp).

---

## 3. Contas bancárias

### ContaBancaria
Toda movimentação financeira passa por uma conta. Titular pode ser um de três tipos (polimorfismo ou FK nullable + enum tipoTitular):
- **Pessoa** — conta pessoal (ex.: do dono, de um gestor de caixa)
- **Cliente** — quando o cliente paga custos diretos da obra pela própria conta
- **EmpresaPropria** — conta operacional de uma das empresas

Campos:
- nome (único)
- tipo: CORRENTE | POUPANCA | CARTAO_CREDITO | CAIXA
- se CARTAO_CREDITO: dia de fechamento, dia de vencimento
- saldo inicial (no momento do cadastro)
- indicador `ehPropria` (bool) — diferencia contas controladas diretamente pelo admin de contas operadas por terceiros (gestores de caixa)

### Sub-contas (contas filhas)
Uma conta pode ter sub-contas vinculadas (auto-relacionamento `contaPaiId` nullable), usadas para organizar internamente o dinheiro (ex.: "Reserva", "Folha", "Operacional" dentro de uma conta de empresa).
- Conta com sub-contas expõe **dois saldos**: saldo próprio (só dela) e saldo total (próprio + soma das sub-contas).

### Cálculo de saldo (tempo real, não persistido como campo mutável simples — calcular ou materializar com cuidado de consistência)
```
saldo = saldo_inicial + recebimentos_confirmados + transferências_recebidas
        − despesas_pagas − transferências_enviadas
```

---

## 4. Despesas (saída de dinheiro)

### Despesa
Toda saída de dinheiro. Duas dimensões possíveis (mutuamente exclusivas):
- **Despesa de obra**: vinculada a Obra + Etapa. Reduz caixa daquela obra.
- **Despesa administrativa**: sem obra, vinculada a uma EmpresaPropria (ex.: pró-labore, conta de luz do escritório).

Campos:
- valor
- data da compra
- data de vencimento (para cartão de crédito: calculada automaticamente a partir do dia de fechamento/vencimento da conta; para boleto: manual)
- data de pagamento (quando efetivamente paga)
- categoria (material, mão de obra, transporte, etc.)
- fornecedor (FK)
- conta bancária de origem
- forma de pagamento: PIX | BOLETO | CARTAO | DINHEIRO
- status: PAGA | A_PAGAR
- indicador de nota fiscal (bool)
- anexos: comprovantes, notas fiscais

### Origem da despesa (enum, imutável após criação — rastreabilidade)
Todas caem na mesma lista, mas a origem fica registrada:
- **MANUAL** — lançada diretamente
- **CONTRATO** — gerada ao registrar pagamento de contrato (empreitada); vinculada a contrato + etapa
- **PRESTACAO_CONTAS** — gerada automaticamente quando uma prestação de contas de gestor é aprovada
- **FOLHA** — gerada automaticamente quando uma folha de pagamento é fechada

### Fluxo de aprovação (despesa pendente)
Quando um funcionário com permissão restrita (gestor de caixa) lança uma despesa, ela não entra direto na lista oficial:
1. Funcionário lança despesa com comprovante anexado → status **PENDENTE**
2. Admin revisa
3. Aprovada → vira despesa oficial (status normal)
4. Rejeitada → registra motivo obrigatório, não gera despesa oficial

### DespesaRecorrente
Template que gera despesas automaticamente na data devida.
- frequência: MENSAL | TRIMESTRAL | SEMESTRAL | ANUAL
- próxima data de geração
- data de fim (opcional, indefinido se nula)
- ativo/inativo (toggle)
- Requer job/scheduler para gerar a despesa real na data.

---

## 5. Recebimentos (entrada de dinheiro)

### Recebimento
Toda entrada de dinheiro. Fontes possíveis: cliente pagando obra (caso mais comum), fornecedor devolvendo dinheiro, rendimento de aplicação, outras entradas administrativas.

Campos:
- valor
- data prevista
- data efetiva (quando entrou na conta)
- pagador (cliente, fornecedor, ou descrição livre — considerar campo texto + FK opcional)
- obra vinculada (opcional — só quando é receita de obra)
- conta bancária de destino
- forma de pagamento
- status: RECEBIDO | A_RECEBER
- anexo: comprovante

### Recebimento parcelado
Parcelas vinculadas como grupo (usar `grupoParcelamentoId` compartilhado entre as parcelas):
- número da parcela atual (ex.: 3 de 10)
- total de parcelas previstas
- cada parcela tem vencimento e status (confirmação) independentes

---

## 6. Contratos com fornecedores

### Contrato
Acordo de valor total com empreiteiro/prestador. Vinculado a uma obra, pode abranger uma ou múltiplas etapas (relação N:N Contrato↔Etapa).
- obra (FK)
- fornecedor (FK)
- descrição do escopo
- valor total contratado (**imutável** após criação — mudanças só via aditivo)
- etapas cobertas
- data de assinatura
- anexo: PDF assinado
- observações

### Pagamento de contrato
Não são parcelas pré-agendadas. Cada pagamento é uma **Despesa comum** com origem CONTRATO, vinculada ao contrato (cronograma real raramente é rígido). Sistema deve calcular: total já pago, saldo a pagar (derivado, somando despesas com origem=CONTRATO e contratoId=X).

### Aditivo contratual
Mudança de valor do contrato (aumento ou redução de escopo) **não edita o contrato original** — o valor base permanece imutável.
- tipo: ACRESCIMO | SUPRESSAO
- valor do aditivo
- descrição/justificativa (obrigatória, mínimo 20 caracteres — validar)
- data
- anexo

```
valor_efetivo_contrato = valor_original + Σ(acréscimos) − Σ(supressões)
```

---

## 7. Folhas de pagamento

### FolhaPagamento
Geradas por obra, com periodicidade **flexível e livre** — cada obra define a sua (semanal, quinzenal, mensal, etc.), sistema não impõe intervalo fixo.
- obra (FK)
- período de referência (definido na criação, não recorrente automático)
- linhas: lista de funcionários com valores calculados

### Tipos de cálculo por funcionário/obra (regra por vínculo, não pelo funcionário globalmente)
- **DIARIA**: valor/dia × dias trabalhados
- **FIXO**: valor pré-definido
- **METRO_QUADRADO**: valor × m² executados

### Valores congelados (regra crítica de modelagem)
No momento da criação da folha, o valor de cada funcionário deve ser **copiado/fotografado** para a linha da folha — não referenciar o valor "vivo" do cadastro. Se o valor base mudar depois, folhas antigas mantêm o valor original.

### Estados da folha (máquina de estados)
```
RASCUNHO → AGUARDANDO_APROVACAO → FECHADA
                                 ↘ REJEITADA (com motivo obrigatório) → volta a RASCUNHO
```
- RASCUNHO: editável livremente
- AGUARDANDO_APROVACAO: submetida pelo gestor para o admin revisar
- FECHADA: aprovada, **imutável a partir daqui** (não pode ser editada, só consultada)
- Ao fechar: sistema gera automaticamente 1 Despesa por funcionário da folha, origem=FOLHA

### Duplicar folha anterior
Copia funcionários e funções da folha anterior, mas recalcula usando os valores **atuais** dos cadastros (não os valores congelados da folha copiada).

---

## 8. Transferências e prestação de contas

### Transferencia
Movimento interno entre duas contas controladas pelo sistema (ex.: empresa → gestor de caixa em campo). **Não é despesa** — o dinheiro continua na operação, só muda de lugar.
- conta origem (FK)
- conta destino (FK)
- obra vinculada
- valor, data, descrição
- anexo: comprovante

**Distinção crítica de modelagem**: Transferência ≠ Despesa. Pagamento de salário/fornecedor (dinheiro sai para terceiro) é **sempre Despesa** (origem MANUAL ou FOLHA), nunca Transferência. Transferência é só entre contas que permanecem operacionais no sistema.

### PrestacaoContas
Quando o gestor de caixa gasta o dinheiro recebido por transferência, presta contas de cada gasto.
- vinculada a uma Transferência específica (FK)
- valor gasto, data, descrição
- anexo: comprovante
- status: PENDENTE | APROVADA | REJEITADA
- Ao aprovar: sistema gera automaticamente uma Despesa oficial da obra, origem=PRESTACAO_CONTAS

### Saldo pendente do gestor (cálculo derivado, por gestor/conta)
```
saldo_pendente = Σ(transferências recebidas) − Σ(prestações aprovadas)
```
Responde "quanto o gestor ainda precisa justificar".

---

## 9. Visão consolidada (saldos — endpoints de agregação)

Três consultas fundamentais que o backend precisa expor:
1. **Saldo por conta bancária** — fórmula da seção 3; contas com sub-contas retornam saldo próprio + saldo total.
2. **Saldo por obra** — total recebido do cliente (recebimentos confirmados da obra) menos total gasto (despesas pagas da obra). Pode ser negativo.
3. **Saldo por empresa própria** — soma de todas as contas + sub-contas vinculadas à empresa.

---

## 10. Auditoria e histórico

### RegistroAuditoria
Log imutável de toda ação relevante: criação, alteração, exclusão, aprovação, rejeição de qualquer registro financeiro.
- usuário (quem fez)
- timestamp (quando)
- estado antes / estado depois (diff — considerar JSON snapshot)
- tabela/registro afetado (entidade + id)

**Regras**: registros de auditoria não podem ser editados nem apagados por ninguém, nem pelo admin — imutabilidade estrutural (ex.: sem endpoint de update/delete nessa tabela; nível de banco também pode reforçar com trigger/permissão).

**Acesso**: página de auditoria com filtros (usuário, período, tipo de operação, tabela) + histórico contextual por registro individual ("o que aconteceu com este aqui").

### Soft delete (exclusão preservada)
Registros financeiros importantes (despesas, recebimentos, contratos, folhas, etc.) **nunca são deletados fisicamente**. "Excluir" = marcar flag `excluido=true` (+ timestamp), registro permanece no banco. Motivo: permitir restauração, manter trilha de auditoria, cumprir retenção legal de dados. Implicação JPA: usar `@Where`/filtro global ou soft-delete pattern em todas as entidades financeiras, nunca `DELETE` físico nessas tabelas.

---

## 11. Anexos e arquivos

Upload aplicável a: contrato assinado da obra, documentos de aditivo, comprovantes de despesa (NF, recibo), comprovantes de transferência, comprovantes de prestação de contas, logos de clientes/empresas.
- Armazenamento seguro, acesso só para usuários autorizados
- Links de acesso com **prazo de validade curto** (URLs assinadas/expiráveis) — sem links públicos permanentes.

---

## 12. Perfis de usuário e permissões

Dois papéis com acesso ao sistema (modelar como enum de role, não como entidades separadas):

### ADMIN
Acesso total.
- CRUD completo de todos os cadastros base (clientes, empresas próprias, pessoas, fornecedores)
- CRUD de obras, etapas, orçamentos
- Cadastra funcionários e define vínculos (empresa/obra, função, regra de pagamento)
- CRUD de contas bancárias e sub-contas
- Lança despesas, recebimentos, contratos, aditivos
- Faz transferências entre contas
- Aprova/rejeita despesas pendentes e prestações de contas
- Aprova/rejeita folhas submetidas
- Cria usuários e define permissões
- Vê toda a auditoria
- Gera relatórios consolidados

**Restrições do próprio admin**: não edita auditoria; não edita folha já fechada (só consulta); não edita valor original de contrato (só aditivo).

### GESTOR_DE_CAIXA (funcionário ativo)
Escopo restrito a obras designadas.
- Vê apenas suas obras designadas
- Lança prestação de contas (com comprovante) das compras que fez
- Lança despesas **pendentes** para a obra designada (aguardam aprovação — não pode lançar despesa oficial direto)
- Lança folhas de pagamento da obra **para aprovação** (não pode fechar sozinho)
- Vê seu próprio saldo pendente (transferências recebidas − prestações aprovadas)

**Restrições**: só vê obras designadas; nunca aprova nada; não vê dados de outras obras/empresas.

→ Nível de autorização deve ser aplicado por linha (row-level: obra designada) e não só por role — tabela de designação Gestor↔Obra (`DesignacaoGestorObra`).

---

## 13. Fluxos de processo (para orquestração de casos de uso/services)

### 13.1 Ciclo completo de uma obra
1. Cadastro inicial: cliente (se novo) → obra (contrato, ART, datas, PDF) → etapas em ordem → orçamento por etapa (mão de obra/material/outros)
2. Estruturação de equipe: define funcionários, função e regra de pagamento por obra; designa gestor(es) de caixa
3. Recebimentos previstos: cadastra parcelas esperadas do cliente (status A_RECEBER até confirmação)
4. Operação diária: despesas lançadas (manual/gestor/contrato); folhas fechadas periodicamente; recebimentos confirmados; transferências para gestores
5. Acompanhamento: consulta saldo; atualiza % de conclusão de etapas; compara real x orçado; aprova pendências
6. Conclusão: última folha fechada, último recebimento confirmado, status→CONCLUIDA, histórico preservado

### 13.2 Ciclo de prestação de contas
1. Admin registra Transferência (empresa → conta do gestor)
2. Gestor compra em campo, guarda comprovantes
3. Gestor lança cada compra como PrestacaoContas vinculada à transferência (status PENDENTE)
4. Admin revisa e aprova/rejeita (rejeição com motivo obrigatório)
5. Aprovação → gera Despesa oficial da obra (origem=PRESTACAO_CONTAS), debita saldo da obra
6. Saldo pendente do gestor recalculado (transferências − prestações aprovadas)

### 13.3 Ciclo de pagamento (folha)
1. Período de trabalho: gestor registra dias trabalhados (diária) ou m² executados (metro quadrado); periodicidade por obra
2. Montagem: duplicar folha anterior (recalcula com valores atuais) ou montar do zero; sistema calcula valor por funcionário conforme regra (diária/fixo/m²)
3. Submissão (se gestor montou): status→AGUARDANDO_APROVACAO
4. Admin revisa: aprova (→FECHADA, imutável) ou rejeita (com motivo, volta ao gestor)
5. Ao fechar: gera 1 Despesa por linha (origem=FOLHA, beneficiário=funcionário, conta definida no fechamento)
6. Pagamento efetivo ocorre fora do sistema (banco); admin marca despesa como PAGA quando confirmado

### 13.4 Rotina mensal típica (referência para dashboards/relatórios)
- Início do mês: saldos de contas/obras/empresas; despesas recorrentes a vencer; recebimentos previstos
- Durante o mês: lançamentos, confirmações, aprovações de pendências/prestações, transferências, aditivos
- Fechamento de folhas: aprovação conforme periodicidade de cada obra; conferência de despesas geradas; marcação de pagas
- Final do mês: comparação real x orçado por obra/etapa; alertas de saldo baixo ou estouro de orçamento; relatórios consolidados

---

## 14. Pontos de atenção para a modelagem JPA

- **Multi-tenant implícito por EmpresaPropria** — decidir se isolamento é por schema, por coluna `empresa_id` com filtro global (`@Filter`/Hibernate), ou aplicação lida com isso no service layer.
- **Soft delete generalizado** nas entidades financeiras (seção 10) — padronizar com uma superclasse `@MappedSuperclass` (`excluido`, `excluidoEm`) ou biblioteca (ex.: Hibernate `@SQLDelete` + `@Where`).
- **Auditoria** — considerar Hibernate Envers ou tabela de auditoria própria com listener JPA (`@EntityListeners`), já que o requisito é trilha completa e imutável, não só campos `createdAt/updatedAt`.
- **Máquinas de estado** (Despesa pendente, Folha, Prestação de Contas) — modelar como enum + validação de transição no service, não deixar o status como campo livre.
- **Valores congelados na folha** — a linha da folha deve armazenar o valor numérico calculado no momento, não apenas uma FK para o cadastro do funcionário/regra.
- **Contrato imutável + aditivo** — nunca fazer UPDATE no valor original do contrato; valor efetivo é sempre calculado (view/query), nunca persistido como campo editável direto.
- **Sub-contas (auto-relacionamento em ContaBancaria)** — cuidado com cálculo recursivo de saldo total; se houver mais de um nível de sub-conta, decidir se o modelo permite hierarquia arbitrária ou só 1 nível (o documento original sugere só 1 nível: conta principal → sub-contas).
- **Contrato↔Etapa** é N:N (um contrato pode cobrir várias etapas) — precisa de tabela de junção.
- **Despesa.origem** é um enum fechado (MANUAL, CONTRATO, PRESTACAO_CONTAS, FOLHA) usado para rastreabilidade — não deve ser alterável após a criação.
- **Autorização por linha** para GESTOR_DE_CAIXA (obras designadas) — implementar via `DesignacaoGestorObra`, checada no service/`@PreAuthorize` customizado, não só por role simples do Spring Security.
- **Funcionario.tipo define o circuito financeiro, não só o papel de acesso** — FOLHA_BASE só tem `VinculoObra` (despesa de obra via folha); GESTOR_CAIXA tem `VinculoEmpresa` (despesa administrativa, seu próprio salário) e pode adicionalmente ter `VinculoObra` (ex.: também trabalha por diária em alguma obra). Não confundir o pagamento do gestor com o dinheiro que ele movimenta em campo (esse só vira despesa de obra via `PrestacaoContas` aprovada).