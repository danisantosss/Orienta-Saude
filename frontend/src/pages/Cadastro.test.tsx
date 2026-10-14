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
      </Routes>
    </MemoryRouter>
  );
}

describe('Cadastro', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(useAuth).mockReturnValue({ registrar, login } as unknown as ReturnType<typeof useAuth>);
  });

  it('mostra erros de validação e não envia dados inválidos', async () => {
    renderizar();
    await userEvent.type(screen.getByLabelText('Nome'), 'M');
    await userEvent.type(screen.getByLabelText('Email'), 'invalido');
    await userEvent.type(screen.getByLabelText('Senha'), '123');
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }));

    expect(screen.getByText('O nome deve ter entre 2 e 120 caracteres.')).toBeInTheDocument();
    expect(screen.getByText('Email inválido.')).toBeInTheDocument();
    expect(screen.getByText('A senha deve ter entre 8 e 128 caracteres.')).toBeInTheDocument();
    expect(registrar).not.toHaveBeenCalled();
  });

  it('cadastra, faz login e redireciona para a triagem', async () => {
    registrar.mockResolvedValue(undefined);
    login.mockResolvedValue(undefined);
    renderizar();
    await userEvent.type(screen.getByLabelText('Nome'), 'Maria Silva');
    await userEvent.type(screen.getByLabelText('Email'), 'maria@exemplo.com');
    await userEvent.type(screen.getByLabelText('Senha'), 'senhaSegura123');
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }));

    expect(registrar).toHaveBeenCalledWith({ nome: 'Maria Silva', email: 'maria@exemplo.com', senha: 'senhaSegura123' });
    expect(login).toHaveBeenCalledWith({ email: 'maria@exemplo.com', senha: 'senhaSegura123' });
    expect(await screen.findByText('Página de triagem')).toBeInTheDocument();
  });

  it('mostra erro geral quando a API falha sem resposta', async () => {
    registrar.mockRejectedValue(new Error('rede'));
    renderizar();
    await userEvent.type(screen.getByLabelText('Nome'), 'Maria Silva');
    await userEvent.type(screen.getByLabelText('Email'), 'maria@exemplo.com');
    await userEvent.type(screen.getByLabelText('Senha'), 'senhaSegura123');
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível concluir o cadastro. Tente novamente.');
  });
});
