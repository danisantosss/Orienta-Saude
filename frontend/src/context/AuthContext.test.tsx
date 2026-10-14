import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import * as authApi from '../api/auth';
import { tokenStorage } from '../api/client';
import { AuthProvider, useAuth } from './AuthContext';

vi.mock('../api/auth');

function criarToken(payload: Record<string, unknown>): string {
  const base64 = (obj: unknown) => btoa(JSON.stringify(obj)).replace(/=+$/, '');
  return `${base64({ alg: 'HS256' })}.${base64(payload)}.assinatura`;
}

const wrapper = ({ children }: { children: ReactNode }) => <AuthProvider>{children}</AuthProvider>;
const tokenValido = criarToken({ sub: '7', email: 'maria@exemplo.com', exp: Date.now() / 1000 + 3600 });

describe('useAuth', () => {
  afterEach(() => {
    localStorage.clear();
    vi.resetAllMocks();
  });

  it('lança erro fora do AuthProvider', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => renderHook(() => useAuth())).toThrow('useAuth deve ser usado dentro de um AuthProvider');
  });

  it('faz login, persiste o token e decodifica o usuário', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: tokenValido, expiraEm: 86400000 });
    const { result } = renderHook(() => useAuth(), { wrapper });

    await act(() => result.current.login({ email: 'maria@exemplo.com', senha: 'senhaSegura123' }));

    expect(result.current.autenticado).toBe(true);
    expect(result.current.usuario).toEqual({ id: 7, email: 'maria@exemplo.com' });
    expect(tokenStorage.get()).toBe(tokenValido);
  });

  it('logout remove o token', async () => {
    tokenStorage.set(tokenValido);
    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.autenticado).toBe(true));

    act(() => result.current.logout());

    expect(result.current.autenticado).toBe(false);
    expect(tokenStorage.get()).toBeNull();
  });

  it('descarta token salvo expirado', async () => {
    tokenStorage.set(criarToken({ sub: '7', email: 'maria@exemplo.com', exp: Date.now() / 1000 - 10 }));
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => expect(result.current.carregando).toBe(false));

    expect(result.current.autenticado).toBe(false);
    expect(tokenStorage.get()).toBeNull();
  });

  it('registrar chama a API de cadastro', async () => {
    vi.mocked(authApi.registrar).mockResolvedValue();
    const { result } = renderHook(() => useAuth(), { wrapper });
    const dados = { nome: 'Maria', email: 'maria@exemplo.com', senha: 'senhaSegura123' };

    await act(() => result.current.registrar(dados));

    expect(authApi.registrar).toHaveBeenCalledWith(dados);
  });
});
