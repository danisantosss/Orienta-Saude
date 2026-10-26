package com.orientasaude.service;

import com.orientasaude.dto.response.PaginaResponse;
import com.orientasaude.dto.response.SessaoTriagemResponse;
import com.orientasaude.exception.EntradaInvalidaException;
import com.orientasaude.model.ResultadoTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.NivelUrgencia;
import com.orientasaude.model.enums.StatusSessao;
import com.orientasaude.repository.SessaoTriagemRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testa as regras do histórico (limites da página, validação do período e conversão
 * para a resposta) com o repositório simulado. Os filtros no banco são testados
 * no teste de integração.
 */
class HistoricoServiceTest {

    private final SessaoTriagemRepository sessaoRepository = mock(SessaoTriagemRepository.class);
    private final HistoricoService service = new HistoricoService(sessaoRepository);
    private final Usuario usuario = Usuario.builder().id(1L).nome("Ana").email("ana@exemplo.com").build();

    @SuppressWarnings("unchecked")
    private Pageable listarECapturarPagina(int pagina, int tamanho) {
        when(sessaoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        service.listar(usuario, pagina, tamanho, null, null, null);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(sessaoRepository).findAll(any(Specification.class), captor.capture());
        return captor.getValue();
    }

    @Test
    void ordenaDaMaisRecenteParaAMaisAntiga() {
        Pageable pageable = listarECapturarPagina(0, 20);

        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Order.desc("iniciadaEm"), Sort.Order.desc("id")));
    }

    @Test
    void limitaAPaginaA20Itens() {
        assertThat(listarECapturarPagina(0, 500).getPageSize()).isEqualTo(20);
    }

    @Test
    void corrigeValoresDePaginaForaDoLimite() {
        Pageable pageable = listarECapturarPagina(-3, 0);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(1);
    }

    @Test
    void dataInicialDepoisDaFinalEhRecusada() {
        LocalDate maio = LocalDate.of(2026, 5, 1);
        LocalDate abril = LocalDate.of(2026, 4, 1);

        assertThatThrownBy(() -> service.listar(usuario, 0, 20, maio, abril, null))
                .isInstanceOf(EntradaInvalidaException.class)
                .hasMessage("A data inicial não pode ser depois da data final");
        verifyNoInteractions(sessaoRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void converteAsSessoesParaAResposta() {
        LocalDateTime iniciada = LocalDateTime.of(2026, 4, 17, 9, 30);
        SessaoTriagem concluida = SessaoTriagem.builder()
                .id(7L).usuario(usuario).sintomasIniciais("Náusea e diarreia").idadePaciente(30)
                .status(StatusSessao.CONCLUIDA).iniciadaEm(iniciada).finalizadaEm(iniciada.plusMinutes(4))
                .build();
        concluida.setResultado(ResultadoTriagem.builder().nivelUrgencia(NivelUrgencia.URGENTE).build());
        SessaoTriagem emProgresso = SessaoTriagem.builder()
                .id(8L).usuario(usuario).sintomasIniciais("Tontura").idadePaciente(30)
                .status(StatusSessao.EM_PROGRESSO).iniciadaEm(iniciada.plusDays(1))
                .build();
        when(sessaoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(emProgresso, concluida), PageRequest.of(0, 2), 5));

        PaginaResponse<SessaoTriagemResponse> resposta = service.listar(usuario, 0, 2, null, null, null);

        assertThat(resposta.itens()).extracting(SessaoTriagemResponse::getId).containsExactly(8L, 7L);
        assertThat(resposta.itens().get(0).getNivelUrgencia()).isNull();
        SessaoTriagemResponse item = resposta.itens().get(1);
        assertThat(item.getNivelUrgencia()).isEqualTo(NivelUrgencia.URGENTE);
        assertThat(item.getSintomasIniciais()).isEqualTo("Náusea e diarreia");
        assertThat(item.getStatus()).isEqualTo(StatusSessao.CONCLUIDA);
        assertThat(item.getIniciadaEm()).isEqualTo(iniciada);
        assertThat(item.getFinalizadaEm()).isEqualTo(iniciada.plusMinutes(4));
        assertThat(resposta.pagina()).isZero();
        assertThat(resposta.tamanho()).isEqualTo(2);
        assertThat(resposta.totalItens()).isEqualTo(5);
        assertThat(resposta.totalPaginas()).isEqualTo(3);
    }
}
