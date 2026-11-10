package com.orientasaude.service;

import com.orientasaude.RelogioAjustavel;
import com.orientasaude.model.LogAuditoria;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.repository.LogAuditoriaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditoriaServiceTest {

    private final LogAuditoriaRepository repository = mock(LogAuditoriaRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-11-09T10:00:00Z"));
    private final AuditoriaService service = new AuditoriaService(repository, transactionManager, 180, relogio);

    @AfterEach
    void limpar() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void gravaSoUsuarioIpTipoEData() {
        service.registrar(TipoEventoAuditoria.LOGIN_FALHO, 7L, "200.1.2.3");

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repository).save(captor.capture());
        LogAuditoria registro = captor.getValue();
        assertThat(registro.getUsuarioId()).isEqualTo(7L);
        assertThat(registro.getIp()).isEqualTo("200.1.2.3");
        assertThat(registro.getTipoEvento()).isEqualTo(TipoEventoAuditoria.LOGIN_FALHO);
        assertThat(registro.getCriadoEm()).isEqualTo(LocalDateTime.of(2026, 11, 9, 10, 0));
    }

    @Test
    void gravaNumaTransacaoPropriaParaSobreviverAoRollbackDeQuemChamou() {
        service.registrar(TipoEventoAuditoria.ACESSO_NEGADO, 7L, "200.1.2.3");

        ArgumentCaptor<TransactionDefinition> captor = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(captor.capture());
        assertThat(captor.getValue().getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void usaOIpDaRequisicaoEmAndamento() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("189.10.20.30");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.registrar(TipoEventoAuditoria.EXCLUSAO_SOLICITADA, null);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIp()).isEqualTo("189.10.20.30");
        assertThat(captor.getValue().getUsuarioId()).isNull();
    }

    @Test
    void foraDeUmaRequisicaoOIpFicaDesconhecido() {
        service.registrar(TipoEventoAuditoria.TOKEN_INVALIDO, null, null);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIp()).isEqualTo(AuditoriaService.IP_DESCONHECIDO);
        assertThat(AuditoriaService.ipDaRequisicaoAtual()).isEqualTo(AuditoriaService.IP_DESCONHECIDO);
    }

    @Test
    void falhaAoGravarNaoDerrubaARequisicao() {
        when(repository.save(any())).thenThrow(new DataAccessResourceFailureException("banco fora"));

        assertThatCode(() -> service.registrar(TipoEventoAuditoria.LOGIN_FALHO, null, "200.1.2.3"))
                .doesNotThrowAnyException();
    }

    @Test
    void removeRegistrosMaisAntigosQueARetencao() {
        when(repository.removerAnterioresA(any())).thenReturn(3);

        service.removerRegistrosAntigos();

        verify(repository).removerAnterioresA(LocalDateTime.of(2026, 5, 13, 10, 0));
    }
}
