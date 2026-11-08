import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from './client';
import { excluirConta, obterPerfil } from './usuario';

vi.mock('./client', () => ({ apiClient: { get: vi.fn(), delete: vi.fn() } }));

describe('api/usuario', () => {
  beforeEach(() => vi.resetAllMocks());

  it('busca o perfil do usuário logado', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { nome: 'Maria' } });

    expect(await obterPerfil()).toEqual({ nome: 'Maria' });
    expect(apiClient.get).toHaveBeenCalledWith('/api/usuario/perfil');
  });

  it('exclui a conta mandando a senha no corpo e devolve a mensagem de confirmação', async () => {
    vi.mocked(apiClient.delete).mockResolvedValue({ data: { mensagem: 'Conta excluída' } });

    expect(await excluirConta('senhaSegura123')).toBe('Conta excluída');
    expect(apiClient.delete).toHaveBeenCalledWith('/api/usuario/conta', { data: { senha: 'senhaSegura123' } });
  });
});
