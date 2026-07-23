package com.mauricio.obracompleta.model;

import com.mauricio.obracompleta.model.enums.StatusObra;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "obra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Obra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String nome;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusObra status = StatusObra.ATIVA;

    private LocalDate dataPrevistaInicio;

    private LocalDate dataPrevistaFim;

    @PositiveOrZero
    @Column(precision = 15, scale = 2)
    private BigDecimal valorContrato;

    @PositiveOrZero
    @Column(precision = 15, scale = 2)
    private BigDecimal orcamentoTotalPrevisto;

    private String numeroContrato;

    private String numeroArt;

    private String endereco;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private String contratoPdfUrl;
}
