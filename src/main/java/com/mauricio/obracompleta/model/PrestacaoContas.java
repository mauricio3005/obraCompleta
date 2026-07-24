package com.mauricio.obracompleta.model;

import com.mauricio.obracompleta.model.enums.StatusPrestacaoContas;
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
@Table(name = "prestacao_contas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrestacaoContas extends RegistroFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferencia_id", nullable = false)
    private Transferencia transferencia;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorGasto;

    @NotNull
    @Column(nullable = false)
    private LocalDate data;

    @NotBlank
    @Column(nullable = false)
    private String descricao;

    private String comprovanteUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusPrestacaoContas status = StatusPrestacaoContas.PENDENTE;

    private String motivoRejeicao;

    @AssertTrue(message = "Motivo da rejeição é obrigatório quando status é REJEITADA")
    private boolean isMotivoRejeicaoValido() {
        if (status != StatusPrestacaoContas.REJEITADA) {
            return true;
        }
        return motivoRejeicao != null && !motivoRejeicao.isBlank();
    }
}
