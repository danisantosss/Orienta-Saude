package com.orientasaude.service;

import com.orientasaude.dto.response.PerfilResponse;
import com.orientasaude.exception.RecursoNaoEncontradoException;
import com.orientasaude.exception.SenhaIncorretaException;
import com.orientasaude.model.Usuario;
import com.orientasaude.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Direitos do titular dos dados (LGPD, art. 18): consultar os próprios dados e excluir a conta.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

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
     * O registro de auditoria não guarda id, nome nem email: só que a exclusão aconteceu.
     */
    @Transactional
    public void excluirConta(Usuario usuario, String senha) {
        Usuario salvo = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada"));

        if (!passwordEncoder.matches(senha, salvo.getSenhaHash())) {
            throw new SenhaIncorretaException();
        }

        usuarioRepository.delete(salvo);
        auditoria.info("Exclusão de conta solicitada pelo titular e concluída");
    }
}
