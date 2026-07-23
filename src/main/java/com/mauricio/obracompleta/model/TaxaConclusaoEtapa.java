package com.mauricio.obracompleta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Append-only: cada atualização gera uma nova linha, nunca sobrescreve uma existente; o percentual atual é o da linha com maior dataRegistro.
@Entity
@Table(name = "taxa_conclusao_etapa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxaConclusaoEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_id", nullable = false)
    private EtapaObra etapa;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentual;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataRegistro;
}
