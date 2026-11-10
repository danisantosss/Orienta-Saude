import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { useAuth } from '../hooks/useAuth';
import Home from './Home';

vi.mock('../hooks/useAuth');

function renderizar(autenticado: boolean) {
  vi.mocked(useAuth).mockReturnValue({ autenticado } as unknown as ReturnType<typeof useAuth>);
  render(
    <MemoryRouter>
      <Home />
    </MemoryRouter>
  );
}

describe('Home', () => {
  it('mostra a ilustração com texto alternativo e leva para o login quem não entrou', () => {
    renderizar(false);

    expect(screen.getByRole('img', { name: /Mulher olhando o celular/ })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Entrar para começar/ })).toHaveAttribute('href', '/login');
  });

  it('leva direto para a triagem quem já entrou', () => {
    renderizar(true);

    expect(screen.getByRole('link', { name: /Iniciar triagem/ })).toHaveAttribute('href', '/triagem');
  });
});
