package com.mauricio.obracompleta.model;

import com.mauricio.obracompleta.model.enums.StatusFolhaPagamento;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Transições de estado (RASCUNHO -> AGUARDANDO_APROVACAO -> FECHADA/REJEITADA) e a imutabilidade de FECHADA são validadas no service, não aqui (ver readme seção 14).
@Entity
@Table(name = "folha_pagamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolhaPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id", nullable = false)
    private Obra obra;

    @NotNull
    @Column(nullable = false)
    private LocalDate periodoInicio;

    @NotNull
    @Column(nullable = false)
    private LocalDate periodoFim;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusFolhaPagamento status = StatusFolhaPagamento.RASCUNHO;

    private String motivoRejeicao;

    @OneToMany(mappedBy = "folha", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LinhaFolhaPagamento> linhas = new ArrayList<>();

    @AssertTrue(message = "periodoFim deve ser igual ou posterior a periodoInicio")
    private boolean isPeriodoValido() {
        return periodoInicio == null || periodoFim == null || !periodoFim.isBefore(periodoInicio);
    }

    @AssertTrue(message = "Motivo da rejeição é obrigatório quando status é REJEITADA")
    private boolean isMotivoRejeicaoValido() {
        if (status != StatusFolhaPagamento.REJEITADA) {
            return true;
        }
        return motivoRejeicao != null && !motivoRejeicao.isBlank();
    }
}
