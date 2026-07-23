package com.mauricio.obracompleta.model;

import com.mauricio.obracompleta.model.enums.TipoCliente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String nome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCliente tipo;

    @CNPJ
    @Column(unique = true)
    private String cnpj;

    @CPF
    @Column(unique = true)
    private String cpf;

    private String logoUrl;

    private String endereco;

    private String telefone;

    @Email
    private String email;

    @AssertTrue(message = "CNPJ é obrigatório para PESSOA_JURIDICA ou ORGAO_PUBLICO, CPF é obrigatório para PESSOA_FISICA")
    private boolean isDocumentoValido() {
        if (tipo == null) {
            return true;
        }
        return switch (tipo) {
            case PESSOA_FISICA -> cpf != null && !cpf.isBlank();
            case PESSOA_JURIDICA, ORGAO_PUBLICO -> cnpj != null && !cnpj.isBlank();
        };
    }
}
