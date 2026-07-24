package com.mauricio.obracompleta.model;

import com.mauricio.obracompleta.model.enums.FormaPagamento;
import com.mauricio.obracompleta.model.enums.OrigemDespesa;
import com.mauricio.obracompleta.model.enums.StatusDespesa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "despesa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Despesa extends RegistroFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @NotNull
    @Column(nullable = false)
    private LocalDate dataCompra;

    private LocalDate dataVencimento;

    private LocalDate dataPagamento;

    @NotBlank
    @Column(nullable = false)
    private String categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_bancaria_id", nullable = false)
    private ContaBancaria contaBancaria;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormaPagamento formaPagamento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDespesa status;

    @NotNull
    @Column(nullable = false)
    @Builder.Default
    private boolean possuiNotaFiscal = false;

    private String comprovanteUrl;

    private String notaFiscalUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private OrigemDespesa origem;

    private String motivoRejeicao;

    // Despesa de obra: obra + etapa preenchidos, empresaPropria nula. Despesa administrativa: só empresaPropria.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_id")
    private EtapaObra etapa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private EmpresaPropria empresaPropria;

    // Preenchido só quando origem=CONTRATO (pagamento de empreitada); demais origens não referenciam contrato.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    // Preenchida só quando origem=PRESTACAO_CONTAS (gerada ao aprovar a prestação de contas do gestor).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestacao_contas_id")
    private PrestacaoContas prestacaoContas;

    // Preenchida só quando origem=FOLHA (gerada ao fechar a folha, 1 despesa por linha/funcionário).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linha_folha_pagamento_id")
    private LinhaFolhaPagamento linhaFolhaPagamento;

    @AssertTrue(message = "Despesa deve ser de obra (obra+etapa) ou administrativa (empresaPropria), nunca as duas")
    private boolean isVinculoValido() {
        boolean deObra = obra != null && etapa != null && empresaPropria == null;
        boolean administrativa = obra == null && etapa == null && empresaPropria != null;
        return deObra || administrativa;
    }

    @AssertTrue(message = "Motivo da rejeição é obrigatório quando status é REJEITADA")
    private boolean isMotivoRejeicaoValido() {
        if (status != StatusDespesa.REJEITADA) {
            return true;
        }
        return motivoRejeicao != null && !motivoRejeicao.isBlank();
    }

    @AssertTrue(message = "contrato só deve ser preenchido quando origem=CONTRATO")
    private boolean isContratoValido() {
        return (origem == OrigemDespesa.CONTRATO) == (contrato != null);
    }

    @AssertTrue(message = "prestacaoContas só deve ser preenchida quando origem=PRESTACAO_CONTAS")
    private boolean isPrestacaoContasValida() {
        return (origem == OrigemDespesa.PRESTACAO_CONTAS) == (prestacaoContas != null);
    }

    @AssertTrue(message = "linhaFolhaPagamento só deve ser preenchida quando origem=FOLHA")
    private boolean isLinhaFolhaPagamentoValida() {
        return (origem == OrigemDespesa.FOLHA) == (linhaFolhaPagamento != null);
    }
}
