package com.orientasaude.service;

import com.orientasaude.dto.request.RegistroRequest;
import com.orientasaude.exception.EmailJaCadastradoException;
import com.orientasaude.model.Usuario;
import com.orientasaude.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável pelo cadastro e autenticação de usuários.
 */
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra um novo usuário no sistema.
     * Valida unicidade do email e armazena a senha com BCrypt.
     *
     * @param request dados do cadastro (nome, email, senha)
     * @return o usuário criado
     * @throws EmailJaCadastradoException se o email já existe
     */
    @Transactional
    public Usuario registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailJaCadastradoException(request.getEmail());
        }

        Usuario usuario = Usuario.builder()
                .nome(request.getNome().trim())
                .email(request.getEmail().trim().toLowerCase())
                .senhaHash(passwordEncoder.encode(request.getSenha()))
                .ativo(true)
                .build();

        return usuarioRepository.save(usuario);
    }
}
