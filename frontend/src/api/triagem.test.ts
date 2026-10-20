import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from './client';
import { LIMITE_REQUISICAO_MS, analisar, encerrar, enviarMensagem, iniciar, listarHistorico, obterResultado } from './triagem';

vi.mock('./client', () => ({ apiClient: { get: vi.fn(), post: vi.fn() } }));

describe('api/triagem', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(apiClient.post).mockResolvedValue({ data: { ok: true } });
    vi.mocked(apiClient.get).mockResolvedValue({ data: { ok: true } });
  });

  it('inicia a triagem com sintomas e idade', async () => {
    const resposta = await iniciar({ sintomas: 'Febre alta desde ontem', idade: 30 });

    expect(apiClient.post).toHaveBeenCalledWith('/api/triagem/iniciar', { sintomas: 'Febre alta desde ontem', idade: 30 },
      { timeout: LIMITE_REQUISICAO_MS });
    expect(resposta).toEqual({ ok: true });
  });

  it('envia a resposta da entrevista', async () => {
    await enviarMensagem(7, 'Desde ontem');

    expect(apiClient.post).toHaveBeenCalledWith('/api/triagem/7/mensagem', { conteudo: 'Desde ontem' },
      { timeout: LIMITE_REQUISICAO_MS });
  });

  it('encerra a entrevista e pede a análise', async () => {
    await encerrar(7);
    await analisar(7);

    expect(apiClient.post).toHaveBeenCalledWith('/api/triagem/7/encerrar', null, { timeout: LIMITE_REQUISICAO_MS });
    expect(apiClient.post).toHaveBeenCalledWith('/api/triagem/7/analisar', null, { timeout: LIMITE_REQUISICAO_MS });
  });

  it('busca o resultado e o histórico com filtros', async () => {
    await obterResultado(7);
    await listarHistorico({ pagina: 1, nivelUrgencia: 'URGENTE' });

    expect(apiClient.get).toHaveBeenCalledWith('/api/triagem/7/resultado');
    expect(apiClient.get).toHaveBeenCalledWith('/api/triagem/historico', { params: { pagina: 1, nivelUrgencia: 'URGENTE' } });
  });

  it('o limite de cada chamada é de 30 segundos', () => {
    expect(LIMITE_REQUISICAO_MS).toBe(30000);
  });
});
