import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from './client';
import { obterIndicadores } from './admin';

describe('api de administração', () => {
  beforeEach(() => vi.restoreAllMocks());

  it('busca os indicadores mandando só as datas preenchidas', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { totalTriagens: 3 } });

    const resposta = await obterIndicadores({ inicio: '2026-11-01', fim: '' });

    expect(get).toHaveBeenCalledWith('/api/admin/indicadores', { params: { inicio: '2026-11-01' } });
    expect(resposta.totalTriagens).toBe(3);
  });

  it('sem filtro não manda parâmetros', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: {} });

    await obterIndicadores();

    expect(get).toHaveBeenCalledWith('/api/admin/indicadores', { params: {} });
  });
});
