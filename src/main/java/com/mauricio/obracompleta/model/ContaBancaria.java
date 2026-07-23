package com.mauricio.obracompleta.model;

import com.mauricio.obracompleta.model.enums.TipoConta;
import com.mauricio.obracompleta.model.enums.TipoTitular;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "conta_bancaria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String nome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConta tipo;

    @Min(1)
    @Max(31)
    private Integer diaFechamento;

    @Min(1)
    @Max(31)
    private Integer diaVencimento;

    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoInicial;

    @NotNull
    @Column(nullable = false)
    @Builder.Default
    private boolean ehPropria = true;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTitular tipoTitular;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "titular_pessoa_id")
    private Pessoa titularPessoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "titular_cliente_id")
    private Cliente titularCliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "titular_empresa_id")
    private EmpresaPropria titularEmpresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_pai_id")
    private ContaBancaria contaPai;

    @AssertTrue(message = "Dia de fechamento e vencimento são obrigatórios apenas para CARTAO_CREDITO")
    private boolean isDiasCartaoValidos() {
        if (tipo != TipoConta.CARTAO_CREDITO) {
            return true;
        }
        return diaFechamento != null && diaVencimento != null;
    }

    @AssertTrue(message = "Exatamente um titular deve ser preenchido, correspondente ao tipoTitular")
    private boolean isTitularValido() {
        if (tipoTitular == null) {
            return true;
        }
        return switch (tipoTitular) {
            case PESSOA -> titularPessoa != null && titularCliente == null && titularEmpresa == null;
            case CLIENTE -> titularCliente != null && titularPessoa == null && titularEmpresa == null;
            case EMPRESA_PROPRIA -> titularEmpresa != null && titularPessoa == null && titularCliente == null;
        };
    }
}
