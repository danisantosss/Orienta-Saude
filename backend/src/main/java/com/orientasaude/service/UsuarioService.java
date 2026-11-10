package com.orientasaude.service;

import com.orientasaude.dto.response.PerfilResponse;
import com.orientasaude.exception.RecursoNaoEncontradoException;
import com.orientasaude.exception.SenhaIncorretaException;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.repository.LogAuditoriaRepository;
import com.orientasaude.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Direitos do titular dos dados (LGPD, art. 18): consultar os próprios dados e excluir a conta.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaRepository logAuditoriaRepository;
    private final AuditoriaService auditoriaService;

    public PerfilResponse obterPerfil(Usuario usuario) {
        return PerfilResponse.builder()
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .criadoEm(usuario.getCriadoEm())
                .consentimentoEm(usuario.getConsentimentoEm())
                .versaoPolitica(usuario.getVersaoPolitica())
                .build();
    }

    /**
     * Apaga a conta na hora. O cascade das entidades remove junto as sessões de triagem,
     * e cada sessão remove suas mensagens e seu resultado. Sem o usuário no banco,
     * o login e qualquer token antigo passam a ser recusados.
     * Na auditoria, os eventos antigos da conta perdem o id do usuário, e a exclusão
     * fica registrada sem id, nome nem email: só que aconteceu, de qual IP e quando.
     */
    @Transactional
    public void excluirConta(Usuario usuario, String senha) {
        Usuario salvo = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada"));

        if (!passwordEncoder.matches(senha, salvo.getSenhaHash())) {
            throw new SenhaIncorretaException();
        }

        usuarioRepository.delete(salvo);
        logAuditoriaRepository.anonimizarUsuario(salvo.getId());
        auditoriaService.registrar(TipoEventoAuditoria.EXCLUSAO_SOLICITADA, null);
    }
}
