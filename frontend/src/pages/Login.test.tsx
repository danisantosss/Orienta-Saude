import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuth } from '../hooks/useAuth';
import Login from './Login';

vi.mock('../hooks/useAuth');

describe('Login', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(useAuth).mockReturnValue({ login: vi.fn() } as unknown as ReturnType<typeof useAuth>);
  });

  it('mostra o aviso recebido de outra página, como a confirmação de conta excluída', () => {
    render(
      <MemoryRouter initialEntries={[{ pathname: '/login', state: { aviso: 'Sua conta foi excluída.' } }]}>
        <Routes><Route path="/login" element={<Login />} /></Routes>
      </MemoryRouter>
    );

    expect(screen.getByRole('status')).toHaveTextContent('Sua conta foi excluída.');
  });
});
