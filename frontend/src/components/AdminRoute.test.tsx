import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { useAuth } from '../hooks/useAuth';
import AdminRoute from './AdminRoute';

vi.mock('../hooks/useAuth');

function renderizar() {
  render(
    <MemoryRouter initialEntries={['/admin']}>
      <Routes>
        <Route path="/login" element={<p>Página de login</p>} />
        <Route element={<AdminRoute />}>
          <Route path="/admin" element={<p>Painel administrativo</p>} />
        </Route>
      </Routes>
    </MemoryRouter>
  );
}

function simularAuth(estado: { autenticado: boolean; ehAdmin: boolean; carregando: boolean }) {
  vi.mocked(useAuth).mockReturnValue({ ...estado } as ReturnType<typeof useAuth>);
}

describe('AdminRoute', () => {
  it('redireciona para /login quando não autenticado', () => {
    simularAuth({ autenticado: false, ehAdmin: false, carregando: false });
    renderizar();
    expect(screen.getByText('Página de login')).toBeInTheDocument();
  });

  it('mostra acesso restrito para quem não é administrador', () => {
    simularAuth({ autenticado: true, ehAdmin: false, carregando: false });
    renderizar();
    expect(screen.getByRole('alert')).toHaveTextContent('só para administradores');
    expect(screen.queryByText('Painel administrativo')).not.toBeInTheDocument();
  });

  it('exibe o painel para o administrador', () => {
    simularAuth({ autenticado: true, ehAdmin: true, carregando: false });
    renderizar();
    expect(screen.getByText('Painel administrativo')).toBeInTheDocument();
  });

  it('exibe carregamento enquanto verifica o token', () => {
    simularAuth({ autenticado: false, ehAdmin: false, carregando: true });
    renderizar();
    expect(screen.getByText('Carregando...')).toBeInTheDocument();
  });
});
