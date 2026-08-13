package com.orientasaude.model;

import com.orientasaude.model.enums.NivelUrgencia;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade que armazena o resultado final de uma triagem.
 * Contém a classificação de urgência, condições possíveis, orientações e disclaimer.
 */
@Entity
@Table(name = "resultados_triagem")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoTriagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", nullable = false, unique = true)
    private SessaoTriagem sessao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_urgencia", nullable = false, length = 15)
    private NivelUrgencia nivelUrgencia;

    @NotBlank
    @Column(name = "condicoes_json", nullable = false, columnDefinition = "JSON")
    private String condicoesJson;

    @NotBlank
    @Column(name = "explicacao_educativa", nullable = false, columnDefinition = "TEXT")
    private String explicacaoEducativa;

    @NotBlank
    @Column(name = "orientacoes_gerais", nullable = false, columnDefinition = "TEXT")
    private String orientacoesGerais;

    @NotBlank
    @Column(name = "sinais_alerta", nullable = false, columnDefinition = "TEXT")
    private String sinaisAlerta;

    @NotBlank
    @Column(name = "quando_buscar_atendimento", nullable = false, columnDefinition = "TEXT")
    private String quandoBuscarAtendimento;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String disclaimer;

    @Column(name = "nota_limitacao", columnDefinition = "TEXT")
    private String notaLimitacao;

    @Column(name = "documentos_rag_usados", columnDefinition = "JSON")
    private String documentosRagUsados;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void aoSerCriado() {
        this.criadoEm = LocalDateTime.now();
    }
}
