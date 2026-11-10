package com.orientasaude.model;

import com.orientasaude.model.enums.PapelUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um usuário cadastrado no sistema.
 * Armazena apenas dados mínimos conforme LGPD: nome, email, senha e o registro do consentimento.
 */
@Entity
@Table(name = "usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 120)
    @Column(nullable = false, length = 120)
    private String nome;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @ToString.Exclude
    @NotBlank
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Builder.Default
    @Column(nullable = false)
    private Boolean ativo = true;

    /**
     * USUARIO por padrão. O DEFAULT na coluna preenche as contas que já existiam quando a coluna
     * foi criada; ADMIN só se atribui no banco ou pela variável APP_ADMIN_EMAILS.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(10) not null default 'USUARIO'")
    private PapelUsuario papel = PapelUsuario.USUARIO;

    /** Quando o usuário aceitou a política de privacidade (prova do consentimento). */
    @Column(name = "consentimento_em")
    private LocalDateTime consentimentoEm;

    /** Versão da política de privacidade que o usuário aceitou. */
    @Column(name = "versao_politica", length = 20)
    private String versaoPolitica;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessaoTriagem> sessoes = new ArrayList<>();

    @PrePersist
    protected void aoSerCriado() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    protected void aoSerAtualizado() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
