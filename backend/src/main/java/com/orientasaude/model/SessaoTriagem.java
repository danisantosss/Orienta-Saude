package com.orientasaude.model;

import com.orientasaude.model.enums.StatusSessao;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa uma sessão completa de triagem.
 * Contém os sintomas iniciais, a idade do paciente e o status do fluxo.
 */
@Entity
@Table(name = "sessoes_triagem", indexes = {
    @Index(name = "idx_sessoes_usuario", columnList = "usuario_id, iniciada_em DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessaoTriagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotBlank
    @Size(min = 10, max = 1000)
    @Column(name = "sintomas_iniciais", nullable = false, length = 1000)
    private String sintomasIniciais;

    @Min(1)
    @Max(120)
    @Column(name = "idade_paciente", nullable = false)
    private Integer idadePaciente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusSessao status = StatusSessao.EM_PROGRESSO;

    @Column(name = "iniciada_em", nullable = false, updatable = false)
    private LocalDateTime iniciadaEm;

    @Column(name = "finalizada_em")
    private LocalDateTime finalizadaEm;

    @OneToMany(mappedBy = "sessao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordemSequencia ASC")
    @Builder.Default
    private List<MensagemTriagem> mensagens = new ArrayList<>();

    @OneToOne(mappedBy = "sessao", cascade = CascadeType.ALL, orphanRemoval = true)
    private ResultadoTriagem resultado;

    @PrePersist
    protected void aoSerCriada() {
        this.iniciadaEm = LocalDateTime.now();
    }
}
