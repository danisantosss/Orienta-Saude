import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuth } from '../hooks/useAuth';
import Layout from './Layout';

vi.mock('../hooks/useAuth');

const logout = vi.fn();

function renderizar(autenticado: boolean, ehAdmin = false) {
  vi.mocked(useAuth).mockReturnValue({
    autenticado,
    ehAdmin,
    usuario: autenticado ? { id: 1, email: 'maria@exemplo.com' } : null,
    logout,
  } as unknown as ReturnType<typeof useAuth>);
  render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<p>Página inicial</p>} />
          <Route path="/login" element={<p>Página de login</p>} />
        </Route>
      </Routes>
    </MemoryRouter>
  );
}

describe('Layout', () => {
  beforeEach(() => vi.resetAllMocks());

  it('sem login, mostra Entrar e Cadastrar e esconde as páginas privadas', () => {
    renderizar(false);
    expect(screen.getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/login');
    expect(screen.getByRole('link', { name: 'Cadastrar' })).toHaveAttribute('href', '/cadastro');
    expect(screen.queryByRole('link', { name: 'Histórico' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Meu perfil' })).not.toBeInTheDocument();
  });

  it('com login, mostra o email, as páginas privadas e sai da conta', async () => {
    renderizar(true);
    expect(screen.getByText('maria@exemplo.com')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Histórico' })).toHaveAttribute('href', '/historico');
    expect(screen.getByRole('link', { name: 'Meu perfil' })).toHaveAttribute('href', '/perfil');
    expect(screen.queryByRole('link', { name: 'Painel' })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Sair' }));
    expect(logout).toHaveBeenCalled();
    expect(screen.getByText('Página de login')).toBeInTheDocument();
  });

  it('mostra o link do painel só para o administrador', () => {
    renderizar(true, true);
    expect(screen.getByRole('link', { name: 'Painel' })).toHaveAttribute('href', '/admin');
  });

  it('abre e fecha o menu do celular', async () => {
    renderizar(false);
    const botao = screen.getByRole('button', { name: 'Abrir menu' });
    expect(botao).toHaveAttribute('aria-expanded', 'false');

    await userEvent.click(botao);
    expect(screen.getByRole('button', { name: 'Fechar menu' })).toHaveAttribute('aria-expanded', 'true');
  });

  it('mostra o "Como funciona?" e o aviso do SAMU no rodapé', () => {
    renderizar(false);
    expect(screen.getByRole('heading', { name: 'Como funciona?' })).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(4);
    expect(screen.getByText(/SAMU \(192\)/)).toBeInTheDocument();
  });

  it('tem link para a Política de Privacidade no rodapé', () => {
    renderizar(false);
    expect(screen.getByRole('link', { name: 'Política de Privacidade' })).toHaveAttribute('href', '/privacidade');
  });
});
