package com.orientasaude.service;

import com.orientasaude.model.LogAuditoria;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.repository.LogAuditoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Grava os eventos de segurança no banco (tabela logs_auditoria) e também no log da aplicação.
 *
 * Cada registro roda numa transação própria (REQUIRES_NEW): quando a triagem de outra pessoa é
 * pedida, a operação lança 404 e desfaz a transação dela, mas o registro da tentativa precisa ficar.
 * Uma falha ao gravar a auditoria nunca derruba a requisição do usuário: vai só para o log.
 */
@Service
public class AuditoriaService {

    static final String IP_DESCONHECIDO = "desconhecido";

    private static final Logger log = LoggerFactory.getLogger("AUDITORIA");

    private final LogAuditoriaRepository repository;
    private final TransactionTemplate transacaoNova;
    private final int retencaoDias;
    private final Clock relogio;

    public AuditoriaService(LogAuditoriaRepository repository,
                            PlatformTransactionManager transactionManager,
                            @Value("${app.auditoria.retencao-dias:180}") int retencaoDias,
                            Clock relogio) {
        this.repository = repository;
        this.transacaoNova = new TransactionTemplate(transactionManager);
        this.transacaoNova.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.retencaoDias = retencaoDias;
        this.relogio = relogio;
    }

    /** Registra o evento com o IP da requisição em andamento. */
    public void registrar(TipoEventoAuditoria tipo, Long usuarioId) {
        registrar(tipo, usuarioId, ipDaRequisicaoAtual());
    }

    public void registrar(TipoEventoAuditoria tipo, Long usuarioId, String ip) {
        String ipRegistrado = ip == null || ip.isBlank() ? IP_DESCONHECIDO : ip;
        log.info("evento={} usuario={} ip={}", tipo, usuarioId, ipRegistrado);
        try {
            transacaoNova.executeWithoutResult(status -> repository.save(LogAuditoria.builder()
                    .usuarioId(usuarioId)
                    .ip(ipRegistrado)
                    .tipoEvento(tipo)
                    .criadoEm(LocalDateTime.now(relogio))
                    .build()));
        } catch (RuntimeException e) {
            log.error("Não foi possível gravar o evento {} no banco: {}", tipo, e.getMessage());
        }
    }

    /**
     * Apaga, todo dia às 3 h, os registros mais antigos que o prazo de retenção
     * (180 dias por padrão; o mínimo exigido é 90).
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void removerRegistrosAntigos() {
        LocalDateTime limite = LocalDateTime.now(relogio).minusDays(retencaoDias);
        int removidos = transacaoNova.execute(status -> repository.removerAnterioresA(limite));
        if (removidos > 0) {
            log.info("{} registros de auditoria com mais de {} dias foram removidos", removidos, retencaoDias);
        }
    }

    /** IP de quem fez a requisição atual; fora de uma requisição HTTP devolve "desconhecido". */
    public static String ipDaRequisicaoAtual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            HttpServletRequest request = atributos.getRequest();
            return request.getRemoteAddr();
        }
        return IP_DESCONHECIDO;
    }
}
