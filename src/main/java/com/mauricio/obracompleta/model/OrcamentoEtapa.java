package com.mauricio.obracompleta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "orcamento_etapa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrcamentoEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_id", nullable = false, unique = true)
    private EtapaObra etapa;

    @PositiveOrZero
    @Column(precision = 15, scale = 2)
    private BigDecimal valorMaoObra;

    @PositiveOrZero
    @Column(precision = 15, scale = 2)
    private BigDecimal valorMaterial;

    @PositiveOrZero
    @Column(precision = 15, scale = 2)
    private BigDecimal valorOutrosCustos;
}
