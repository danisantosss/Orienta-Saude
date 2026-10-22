import { act, renderHook } from '@testing-library/react';
import { AxiosError, AxiosHeaders } from 'axios';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as triagemApi from '../api/triagem';
import type { ResultadoTriagemResponse } from '../types';
import { useTriagem } from './useTriagem';

vi.mock('../api/triagem');

function erroHttp(status: number, data: Record<string, unknown>): AxiosError {
  return new AxiosError('erro', 'ERR_BAD_RESPONSE', undefined, undefined, {
    status, data, statusText: '', headers: {}, config: { headers: new AxiosHeaders() },
  });
}

const resultado = { sessaoId: 7 } as ResultadoTriagemResponse;

describe('useTriagem', () => {
  beforeEach(() => {
    vi.mocked(triagemApi.iniciar).mockResolvedValue({ sessaoId: 7, primeiraPergunta: 'Há quanto tempo?' });
    vi.mocked(triagemApi.analisar).mockResolvedValue(resultado);
  });

  afterEach(() => {
    vi.resetAllMocks();
    vi.useRealTimers();
  });

  it('inicia a triagem e mostra a primeira pergunta', async () => {
    const { result } = renderHook(() => useTriagem());

    await act(() => result.current.iniciar({ sintomas: 'Febre alta desde ontem', idade: 30 }));

    expect(result.current.sessaoId).toBe(7);
    expect(result.current.mensagens).toEqual([{ papel: 'ASSISTENTE', conteudo: 'Há quanto tempo?' }]);
    expect(result.current.carregando).toBe(false);
  });

  it('envia a resposta e acrescenta a próxima pergunta', async () => {
    vi.mocked(triagemApi.enviarMensagem).mockResolvedValue({ pergunta: 'Tem tosse?', completa: false, aviso: null });
    const { result } = renderHook(() => useTriagem());
    await act(() => result.current.iniciar({ sintomas: 'Febre alta desde ontem', idade: 30 }));

    await act(() => result.current.responder('Dois dias'));

    expect(triagemApi.enviarMensagem).toHaveBeenCalledWith(7, 'Dois dias');
    expect(result.current.mensagens.map((m) => m.conteudo)).toEqual(['Há quanto tempo?', 'Dois dias', 'Tem tosse?']);
    expect(result.current.mensagens[1].papel).toBe('USUARIO');
  });

  it('quando a entrevista termina, pede a análise sozinho e marca como concluída', async () => {
    vi.mocked(triagemApi.enviarMensagem).mockResolvedValue({ pergunta: null, completa: true, aviso: 'Coleta inconclusiva.' });
    const { result } = renderHook(() => useTriagem());
    await act(() => result.current.iniciar({ sintomas: 'Febre alta desde ontem', idade: 30 }));

    await act(() => result.current.responder('Dois dias'));

    expect(result.current.completa).toBe(true);
    expect(result.current.aviso).toBe('Coleta inconclusiva.');
    expect(triagemApi.analisar).toHaveBeenCalledWith(7);
    expect(result.current.concluida).toBe(true);
  });

  it('encerrar antes da hora também gera a análise', async () => {
    vi.mocked(triagemApi.encerrar).mockResolvedValue({ pergunta: null, completa: true, aviso: 'Encerrada antes.' });
    const { result } = renderHook(() => useTriagem());
    await act(() => result.current.iniciar({ sintomas: 'Febre alta desde ontem', idade: 30 }));

    await act(() => result.current.encerrar());

    expect(triagemApi.encerrar).toHaveBeenCalledWith(7);
    expect(result.current.concluida).toBe(true);
  });

  it('falha da IA mostra o erro, desfaz a resposta e permite repetir', async () => {
    vi.mocked(triagemApi.enviarMensagem)
      .mockRejectedValueOnce(erroHttp(503, { mensagem: 'Serviço temporariamente indisponível.' }))
      .mockResolvedValueOnce({ pergunta: 'Tem tosse?', completa: false, aviso: null });
    const { result } = renderHook(() => useTriagem());
    await act(() => result.current.iniciar({ sintomas: 'Febre alta desde ontem', idade: 30 }));

    await act(() => result.current.responder('Dois dias'));

    expect(result.current.erro).toBe('Serviço temporariamente indisponível.');
    expect(result.current.mensagens).toHaveLength(1);

    await act(() => result.current.repetir());

    expect(triagemApi.enviarMensagem).toHaveBeenLastCalledWith(7, 'Dois dias');
    expect(result.current.erro).toBeNull();
    expect(result.current.mensagens).toHaveLength(3);
  });

  it('falha na análise permite repetir só a análise', async () => {
    vi.mocked(triagemApi.encerrar).mockResolvedValue({ pergunta: null, completa: true, aviso: null });
    vi.mocked(triagemApi.analisar).mockRejectedValueOnce(erroHttp(503, {})).mockResolvedValueOnce(resultado);
    const { result } = renderHook(() => useTriagem());
    await act(() => result.current.iniciar({ sintomas: 'Febre alta desde ontem', idade: 30 }));

    await act(() => result.current.encerrar());
    expect(result.current.erro).toContain('temporariamente indisponível');
    expect(result.current.concluida).toBe(false);

    await act(() => result.current.repetir());

    expect(triagemApi.encerrar).toHaveBeenCalledTimes(1);
    expect(triagemApi.analisar).toHaveBeenCalledTimes(2);
    expect(result.current.concluida).toBe(true);
  });

  it('erros de validação do backend ficam por campo', async () => {
    vi.mocked(triagemApi.iniciar).mockRejectedValue(
      erroHttp(400, { mensagem: 'Erro de validação', campos: { idade: 'A idade deve estar entre 1 e 120' } }));
    const { result } = renderHook(() => useTriagem());

    await act(() => result.current.iniciar({ sintomas: 'Febre alta desde ontem', idade: 300 }));

    expect(result.current.errosCampos).toEqual({ idade: 'A idade deve estar entre 1 e 120' });
    expect(result.current.sessaoId).toBeNull();
  });

  it('operação cancelada por tempo avisa que passou de 30 segundos', async () => {
    vi.mocked(triagemApi.iniciar).mockRejectedValue(new AxiosError('timeout', 'ECONNABORTED'));
    const { result } = renderHook(() => useTriagem());

    await act(() => result.current.iniciar({ sintomas: 'Febre alta desde ontem', idade: 30 }));

    expect(result.current.erro).toContain('30 segundos');
  });

  it('avisa que está demorando depois de 10 segundos', async () => {
    vi.useFakeTimers();
    let concluir: (valor: { sessaoId: number; primeiraPergunta: string }) => void = () => {};
    vi.mocked(triagemApi.iniciar).mockReturnValue(new Promise((resolver) => { concluir = resolver; }));
    const { result } = renderHook(() => useTriagem());

    let promessa: Promise<void> = Promise.resolve();
    act(() => { promessa = result.current.iniciar({ sintomas: 'Febre alta desde ontem', idade: 30 }); });
    expect(result.current.carregando).toBe(true);
    expect(result.current.demorando).toBe(false);

    act(() => { vi.advanceTimersByTime(10000); });
    expect(result.current.demorando).toBe(true);

    await act(async () => { concluir({ sessaoId: 7, primeiraPergunta: 'Há quanto tempo?' }); await promessa; });
    expect(result.current.demorando).toBe(false);
    expect(result.current.carregando).toBe(false);
  });
});
