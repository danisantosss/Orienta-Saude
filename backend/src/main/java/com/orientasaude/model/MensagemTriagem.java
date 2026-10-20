package com.orientasaude.model;

import com.orientasaude.model.enums.PapelMensagem;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma mensagem na entrevista adaptativa.
 * Pode ser do SISTEMA (sintomas iniciais), USUARIO (resposta) ou ASSISTENTE (pergunta da IA).
 */
@Entity
@Table(name = "mensagens_triagem", indexes = {
    @Index(name = "idx_mensagens_sessao", columnList = "sessao_id, ordem_sequencia")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensagemTriagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", nullable = false)
    private SessaoTriagem sessao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PapelMensagem papel;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "ordem_sequencia", nullable = false)
    private Integer ordemSequencia;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm;

    /**
     * Regra única de contagem da entrevista: toda mensagem do ASSISTENTE é uma pergunta.
     * Avisos e orientações vão na resposta da API e nunca são gravados como mensagem,
     * por isso os limites de 2 e 10 perguntas contam só este papel.
     */
    public boolean isPerguntaDaEntrevista() {
        return papel == PapelMensagem.ASSISTENTE;
    }

    @PrePersist
    protected void aoSerCriada() {
        this.criadaEm = LocalDateTime.now();
    }
}
