package com.mauricio.obracompleta.model;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

import java.time.LocalDateTime;

// Soft delete estrutural: Hibernate mapeia a coluna "excluido" e converte delete em update, filtrando automaticamente nas queries.
@MappedSuperclass
@SoftDelete(columnName = "excluido")
@Getter
@Setter
public abstract class RegistroFinanceiro {

    private LocalDateTime excluidoEm;
}
