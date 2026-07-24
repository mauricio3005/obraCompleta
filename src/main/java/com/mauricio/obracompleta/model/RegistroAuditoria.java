package com.mauricio.obracompleta.model;

import com.mauricio.obracompleta.model.enums.TipoOperacaoAuditoria;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Imutável por natureza: todas as colunas são updatable=false e não há (nem deve haver) endpoint de update/delete para esta entidade, nem para ADMIN.
@Entity
@Table(name = "registro_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoOperacaoAuditoria tipoOperacao;

    @NotBlank
    @Column(nullable = false, updatable = false)
    private String entidade;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Long entidadeId;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String estadoAntes;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String estadoDepois;

    @AssertTrue(message = "estadoAntes/estadoDepois devem ser consistentes com tipoOperacao (CRIACAO só depois, EXCLUSAO só antes, demais ambos)")
    private boolean isEstadosValidos() {
        if (tipoOperacao == null) {
            return true;
        }
        return switch (tipoOperacao) {
            case CRIACAO -> estadoAntes == null && estadoDepois != null;
            case EXCLUSAO -> estadoAntes != null && estadoDepois == null;
            case ALTERACAO, APROVACAO, REJEICAO -> estadoAntes != null && estadoDepois != null;
        };
    }
}
