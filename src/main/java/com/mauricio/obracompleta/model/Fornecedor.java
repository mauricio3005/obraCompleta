package com.mauricio.obracompleta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;

@Entity
@Table(name = "fornecedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String nome;

    @NotBlank
    private String categoria;

    private String contato;

    @CNPJ
    @Column(unique = true)
    private String cnpj;

    @CPF
    @Column(unique = true)
    private String cpf;

    private String telefone;

    @Email
    private String email;

    @AssertTrue(message = "Informe ao menos um documento: CNPJ ou CPF")
    private boolean isDocumentoInformado() {
        return (cnpj != null && !cnpj.isBlank()) || (cpf != null && !cpf.isBlank());
    }
}
