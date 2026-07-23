package com.mauricio.obracompleta.model;

import com.mauricio.obracompleta.model.enums.FormaPagamento;
import com.mauricio.obracompleta.model.enums.FrequenciaRecorrencia;
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
@Table(name = "despesa_recorrente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespesaRecorrente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

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
    @Column(nullable = false)
    @Builder.Default
    private boolean possuiNotaFiscal = false;

    // Mesma regra de exclusividade da Despesa: obra+etapa (de obra) ou só empresaPropria (administrativa).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_id")
    private EtapaObra etapa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private EmpresaPropria empresaPropria;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FrequenciaRecorrencia frequencia;

    @NotNull
    @Column(nullable = false)
    private LocalDate proximaDataGeracao;

    private LocalDate dataFim;

    @NotNull
    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @AssertTrue(message = "Despesa recorrente deve ser de obra (obra+etapa) ou administrativa (empresaPropria), nunca as duas")
    private boolean isVinculoValido() {
        boolean deObra = obra != null && etapa != null && empresaPropria == null;
        boolean administrativa = obra == null && etapa == null && empresaPropria != null;
        return deObra || administrativa;
    }
}
