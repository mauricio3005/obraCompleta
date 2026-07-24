package com.mauricio.obracompleta.model;

import com.mauricio.obracompleta.model.enums.RegraPagamento;
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

// funcao, regraPagamento e valorCongelado são copiados do VinculoObra no momento da criação da linha — nunca referenciar o valor "vivo" do vínculo depois.
@Entity
@Table(name = "linha_folha_pagamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinhaFolhaPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folha_id", nullable = false)
    private FolhaPagamento folha;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @NotBlank
    @Column(nullable = false)
    private String funcao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegraPagamento regraPagamento;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorCongelado;

    private Integer diasTrabalhados;

    @Column(precision = 10, scale = 2)
    private BigDecimal metrosQuadrados;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorCalculado;

    @AssertTrue(message = "diasTrabalhados/metrosQuadrados devem corresponder à regraPagamento (DIARIA usa dias, METRO_QUADRADO usa m², FIXO usa nenhum)")
    private boolean isMedidaValida() {
        return switch (regraPagamento) {
            case DIARIA -> diasTrabalhados != null && metrosQuadrados == null;
            case METRO_QUADRADO -> metrosQuadrados != null && diasTrabalhados == null;
            case FIXO -> diasTrabalhados == null && metrosQuadrados == null;
        };
    }
}
