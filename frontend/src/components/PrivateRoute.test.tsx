import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { useAuth } from '../hooks/useAuth';
import PrivateRoute from './PrivateRoute';

vi.mock('../hooks/useAuth');

function renderizar() {
  render(
    <MemoryRouter initialEntries={['/triagem']}>
      <Routes>
        <Route path="/login" element={<p>Página de login</p>} />
        <Route element={<PrivateRoute />}>
          <Route path="/triagem" element={<p>Área privada</p>} />
        </Route>
      </Routes>
    </MemoryRouter>
  );
}

function simularAuth(estado: { autenticado: boolean; carregando: boolean }) {
  vi.mocked(useAuth).mockReturnValue({ ...estado } as ReturnType<typeof useAuth>);
}

describe('PrivateRoute', () => {
  it('redireciona para /login quando não autenticado', () => {
    simularAuth({ autenticado: false, carregando: false });
    renderizar();
    expect(screen.getByText('Página de login')).toBeInTheDocument();
  });

  it('exibe a rota privada quando autenticado', () => {
    simularAuth({ autenticado: true, carregando: false });
    renderizar();
    expect(screen.getByText('Área privada')).toBeInTheDocument();
  });

  it('exibe carregamento enquanto verifica o token', () => {
    simularAuth({ autenticado: false, carregando: true });
    renderizar();
    expect(screen.getByText('Carregando...')).toBeInTheDocument();
  });
});
