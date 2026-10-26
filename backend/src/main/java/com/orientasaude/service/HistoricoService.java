package com.orientasaude.service;

import com.orientasaude.dto.response.PaginaResponse;
import com.orientasaude.dto.response.SessaoTriagemResponse;
import com.orientasaude.exception.EntradaInvalidaException;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.NivelUrgencia;
import com.orientasaude.repository.SessaoTriagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.orientasaude.repository.SessaoTriagemSpecifications.comNivelUrgencia;
import static com.orientasaude.repository.SessaoTriagemSpecifications.doUsuario;
import static com.orientasaude.repository.SessaoTriagemSpecifications.iniciadaAPartirDe;
import static com.orientasaude.repository.SessaoTriagemSpecifications.iniciadaAntesDe;

/**
 * Histórico de triagens do usuário autenticado, com paginação e filtros opcionais
 * por período e nível de urgência.
 */
@Service
@RequiredArgsConstructor
public class HistoricoService {

    public static final int TAMANHO_MAXIMO_PAGINA = 20;

    private static final Sort MAIS_RECENTES_PRIMEIRO = Sort.by(Sort.Order.desc("iniciadaEm"), Sort.Order.desc("id"));

    private final SessaoTriagemRepository sessaoRepository;

    /**
     * O usuário vem sempre do token: o filtro por dono é obrigatório e os demais só entram
     * quando preenchidos. As datas são dias inteiros: o fim inclui o dia todo.
     */
    @Transactional(readOnly = true)
    public PaginaResponse<SessaoTriagemResponse> listar(Usuario usuario, int pagina, int tamanho,
                                                        LocalDate inicio, LocalDate fim, NivelUrgencia nivelUrgencia) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new EntradaInvalidaException("A data inicial não pode ser depois da data final");
        }

        Specification<SessaoTriagem> filtro = doUsuario(usuario.getId());
        if (inicio != null) {
            filtro = filtro.and(iniciadaAPartirDe(inicio.atStartOfDay()));
        }
        if (fim != null) {
            filtro = filtro.and(iniciadaAntesDe(fim.plusDays(1).atStartOfDay()));
        }
        if (nivelUrgencia != null) {
            filtro = filtro.and(comNivelUrgencia(nivelUrgencia));
        }

        PageRequest paginacao = PageRequest.of(Math.max(pagina, 0),
                Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO_PAGINA), MAIS_RECENTES_PRIMEIRO);
        return PaginaResponse.de(sessaoRepository.findAll(filtro, paginacao), this::paraResposta);
    }

    private SessaoTriagemResponse paraResposta(SessaoTriagem sessao) {
        return SessaoTriagemResponse.builder()
                .id(sessao.getId())
                .sintomasIniciais(sessao.getSintomasIniciais())
                .idadePaciente(sessao.getIdadePaciente())
                .status(sessao.getStatus())
                .iniciadaEm(sessao.getIniciadaEm())
                .finalizadaEm(sessao.getFinalizadaEm())
                .nivelUrgencia(sessao.getResultado() != null ? sessao.getResultado().getNivelUrgencia() : null)
                .build();
    }
}
