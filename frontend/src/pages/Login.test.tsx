import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AxiosError, AxiosHeaders } from 'axios';
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

  it('mostra o aviso de login bloqueado depois de muitas tentativas', async () => {
    const mensagem = 'Muitas tentativas de login sem sucesso. Tente novamente em 15 minutos.';
    const login = vi.fn().mockRejectedValue(new AxiosError('bloqueado', '429', undefined, undefined, {
      status: 429, statusText: 'Too Many Requests', headers: {}, config: { headers: new AxiosHeaders() },
      data: { mensagem },
    }));
    vi.mocked(useAuth).mockReturnValue({ login } as unknown as ReturnType<typeof useAuth>);
    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes><Route path="/login" element={<Login />} /></Routes>
      </MemoryRouter>
    );

    await userEvent.type(screen.getByLabelText('Email'), 'maria@exemplo.com');
    await userEvent.type(screen.getByLabelText('Senha'), 'senhaErrada99');
    await userEvent.click(screen.getByRole('button', { name: /Entrar/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent(mensagem);
  });
});
