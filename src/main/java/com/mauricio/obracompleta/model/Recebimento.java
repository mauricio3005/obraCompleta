package com.mauricio.obracompleta.model;

import com.mauricio.obracompleta.model.enums.FormaPagamento;
import com.mauricio.obracompleta.model.enums.StatusRecebimento;
import com.mauricio.obracompleta.model.enums.TipoPagador;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "recebimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recebimento extends RegistroFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @NotNull
    @Column(nullable = false)
    private LocalDate dataPrevista;

    private LocalDate dataEfetiva;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPagador tipoPagador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagador_cliente_id")
    private Cliente pagadorCliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagador_fornecedor_id")
    private Fornecedor pagadorFornecedor;

    private String pagadorDescricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id")
    private Obra obra;

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
    private StatusRecebimento status;

    private String comprovanteUrl;

    // Compartilhado entre as parcelas de um mesmo recebimento parcelado; nulo quando não é parcelado.
    private UUID grupoParcelamentoId;

    private Integer numeroParcela;

    private Integer totalParcelas;

    @AssertTrue(message = "pagadorCliente/pagadorFornecedor/pagadorDescricao devem corresponder ao tipoPagador")
    private boolean isPagadorValido() {
        return switch (tipoPagador) {
            case CLIENTE -> pagadorCliente != null && pagadorFornecedor == null;
            case FORNECEDOR -> pagadorFornecedor != null && pagadorCliente == null;
            case OUTRO -> pagadorCliente == null && pagadorFornecedor == null
                    && pagadorDescricao != null && !pagadorDescricao.isBlank();
        };
    }

    @AssertTrue(message = "grupoParcelamentoId, numeroParcela e totalParcelas devem estar todos presentes ou todos ausentes")
    private boolean isParcelamentoValido() {
        boolean todosPresentes = grupoParcelamentoId != null && numeroParcela != null && totalParcelas != null;
        boolean todosAusentes = grupoParcelamentoId == null && numeroParcela == null && totalParcelas == null;
        if (!todosPresentes && !todosAusentes) {
            return false;
        }
        return !todosPresentes || (numeroParcela >= 1 && numeroParcela <= totalParcelas);
    }
}
