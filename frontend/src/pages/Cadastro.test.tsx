import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuth } from '../hooks/useAuth';
import Cadastro from './Cadastro';

vi.mock('../hooks/useAuth');

const registrar = vi.fn();
const login = vi.fn();

function renderizar() {
  render(
    <MemoryRouter initialEntries={['/cadastro']}>
      <Routes>
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="/triagem" element={<p>Página de triagem</p>} />
        <Route path="/privacidade" element={<p>Página da política</p>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('Cadastro', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(useAuth).mockReturnValue({ registrar, login } as unknown as ReturnType<typeof useAuth>);
  });

  it('mostra a ilustração do cadastro com texto alternativo', () => {
    renderizar();

    expect(screen.getByRole('img', { name: /Mulher sorrindo enquanto usa o celular/ })).toBeInTheDocument();
  });

  it('mostra erros de validação e não envia dados inválidos', async () => {
    renderizar();
    await userEvent.type(screen.getByLabelText('Nome completo'), 'M');
    await userEvent.type(screen.getByLabelText('Email'), 'invalido');
    await userEvent.type(screen.getByLabelText('Senha'), '123');
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta e continuar' }));

    expect(screen.getByText('O nome deve ter entre 2 e 120 caracteres.')).toBeInTheDocument();
    expect(screen.getByText('Email inválido.')).toBeInTheDocument();
    expect(screen.getByText('A senha deve ter entre 8 e 128 caracteres.')).toBeInTheDocument();
    expect(screen.getByText('Para criar a conta, aceite a Política de Privacidade e os Termos de Uso.')).toBeInTheDocument();
    expect(registrar).not.toHaveBeenCalled();
  });

  it('cadastra, faz login e redireciona para a triagem', async () => {
    registrar.mockResolvedValue(undefined);
    login.mockResolvedValue(undefined);
    renderizar();
    await userEvent.type(screen.getByLabelText('Nome completo'), 'Maria Silva');
    await userEvent.type(screen.getByLabelText('Email'), 'maria@exemplo.com');
    await userEvent.type(screen.getByLabelText('Senha'), 'senhaSegura123');
    await userEvent.type(screen.getByLabelText('Confirmar senha'), 'senhaSegura123');
    await userEvent.click(screen.getByRole('checkbox'));
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta e continuar' }));

    expect(registrar).toHaveBeenCalledWith({
      nome: 'Maria Silva', email: 'maria@exemplo.com', senha: 'senhaSegura123', consentimento: true,
    });
    expect(login).toHaveBeenCalledWith({ email: 'maria@exemplo.com', senha: 'senhaSegura123' });
    expect(await screen.findByText('Página de triagem')).toBeInTheDocument();
  });

  it('mostra erro geral quando a API falha sem resposta', async () => {
    registrar.mockRejectedValue(new Error('rede'));
    renderizar();
    await userEvent.type(screen.getByLabelText('Nome completo'), 'Maria Silva');
    await userEvent.type(screen.getByLabelText('Email'), 'maria@exemplo.com');
    await userEvent.type(screen.getByLabelText('Senha'), 'senhaSegura123');
    await userEvent.type(screen.getByLabelText('Confirmar senha'), 'senhaSegura123');
    await userEvent.click(screen.getByRole('checkbox'));
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta e continuar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível concluir o cadastro. Tente novamente.');
  });

  it('não envia quando a confirmação é diferente da senha', async () => {
    renderizar();
    await userEvent.type(screen.getByLabelText('Nome completo'), 'Maria Silva');
    await userEvent.type(screen.getByLabelText('Email'), 'maria@exemplo.com');
    await userEvent.type(screen.getByLabelText('Senha'), 'senhaSegura123');
    await userEvent.type(screen.getByLabelText('Confirmar senha'), 'outraSenha123');
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta e continuar' }));

    expect(screen.getByText('As senhas não são iguais.')).toBeInTheDocument();
    expect(registrar).not.toHaveBeenCalled();
  });

  it('não envia sem o aceite da política e tem link para lê-la', async () => {
    renderizar();
    await userEvent.type(screen.getByLabelText('Nome completo'), 'Maria Silva');
    await userEvent.type(screen.getByLabelText('Email'), 'maria@exemplo.com');
    await userEvent.type(screen.getByLabelText('Senha'), 'senhaSegura123');
    await userEvent.type(screen.getByLabelText('Confirmar senha'), 'senhaSegura123');
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta e continuar' }));

    expect(screen.getByText('Para criar a conta, aceite a Política de Privacidade e os Termos de Uso.')).toBeInTheDocument();
    expect(registrar).not.toHaveBeenCalled();
    expect(screen.getByRole('link', { name: 'Política de Privacidade e os Termos de Uso' }))
      .toHaveAttribute('href', '/privacidade');
  });
});
