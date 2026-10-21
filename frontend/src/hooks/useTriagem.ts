import { useCallback, useRef, useState } from 'react';
import axios from 'axios';
import * as triagemApi from '../api/triagem';
import type { ErroResponse, IniciarTriagemRequest, MensagemTriagemResponse } from '../types';

export interface MensagemChat {
  papel: 'ASSISTENTE' | 'USUARIO';
  conteudo: string;
}

/** Depois desse tempo a tela avisa que está demorando mais que o esperado. */
export const AVISO_DEMORA_MS = 10000;

const ERRO_TEMPO_ESGOTADO = 'A operação passou de 30 segundos e foi cancelada. Tente novamente.';
const ERRO_IA_INDISPONIVEL = 'O serviço de orientação está temporariamente indisponível. Tente novamente.';
const ERRO_GENERICO = 'Não foi possível concluir. Verifique sua conexão e tente novamente.';

/**
 * Estado e ações da triagem: início, entrevista, encerramento e análise.
 * Quando a entrevista termina, a análise é pedida automaticamente.
 * Se algo falhar, a última ação fica guardada e `repetir` a executa de novo,
 * sem perder o que já foi coletado.
 */
export function useTriagem() {
  const [sessaoId, setSessaoId] = useState<number | null>(null);
  const [mensagens, setMensagens] = useState<MensagemChat[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [demorando, setDemorando] = useState(false);
  const [completa, setCompleta] = useState(false);
  const [concluida, setConcluida] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [errosCampos, setErrosCampos] = useState<Record<string, string>>({});
  const ultimaAcao = useRef<(() => Promise<void>) | null>(null);

  /** Executa uma chamada com indicador de carregamento e aviso de demora; devolve null se falhar. */
  const executar = useCallback(async <T,>(chamada: () => Promise<T>): Promise<T | null> => {
    setErro(null);
    setErrosCampos({});
    setCarregando(true);
    setDemorando(false);
    const temporizador = setTimeout(() => setDemorando(true), AVISO_DEMORA_MS);
    try {
      return await chamada();
    } catch (e) {
      tratarErro(e, setErro, setErrosCampos);
      return null;
    } finally {
      clearTimeout(temporizador);
      setCarregando(false);
      setDemorando(false);
    }
  }, []);

  /** Guarda a ação para o botão "tentar novamente" e a executa. */
  const registrarEExecutar = useCallback(async (acao: () => Promise<void>) => {
    ultimaAcao.current = acao;
    await acao();
  }, []);

  const analisar = useCallback((id: number) => registrarEExecutar(async () => {
    const resultado = await executar(() => triagemApi.analisar(id));
    if (resultado) setConcluida(true);
  }), [executar, registrarEExecutar]);

  const tratarFimDeRodada = useCallback(async (id: number, resposta: MensagemTriagemResponse) => {
    if (!resposta.completa) {
      setMensagens((atuais) => [...atuais, { papel: 'ASSISTENTE', conteudo: resposta.pergunta ?? '' }]);
      return;
    }
    setCompleta(true);
    setAviso(resposta.aviso);
    await analisar(id);
  }, [analisar]);

  const iniciar = useCallback((dados: IniciarTriagemRequest) => registrarEExecutar(async () => {
    const resposta = await executar(() => triagemApi.iniciar(dados));
    if (resposta) {
      setSessaoId(resposta.sessaoId);
      setMensagens([{ papel: 'ASSISTENTE', conteudo: resposta.primeiraPergunta }]);
    }
  }), [executar, registrarEExecutar]);

  const responder = useCallback(async (conteudo: string) => {
    if (sessaoId === null) return;
    const id = sessaoId;
    await registrarEExecutar(async () => {
      setMensagens((atuais) => [...atuais, { papel: 'USUARIO', conteudo }]);
      const resposta = await executar(() => triagemApi.enviarMensagem(id, conteudo));
      if (!resposta) {
        // O backend desfez a gravação da resposta; ela sai do chat e volta com "repetir"
        setMensagens((atuais) => atuais.slice(0, -1));
        return;
      }
      await tratarFimDeRodada(id, resposta);
    });
  }, [sessaoId, executar, registrarEExecutar, tratarFimDeRodada]);

  const encerrar = useCallback(async () => {
    if (sessaoId === null) return;
    const id = sessaoId;
    await registrarEExecutar(async () => {
      const resposta = await executar(() => triagemApi.encerrar(id));
      if (resposta) await tratarFimDeRodada(id, resposta);
    });
  }, [sessaoId, executar, registrarEExecutar, tratarFimDeRodada]);

  const repetir = useCallback(async () => {
    if (ultimaAcao.current) await ultimaAcao.current();
  }, []);

  return {
    sessaoId, mensagens, carregando, demorando, completa, concluida, aviso, erro, errosCampos,
    iniciar, responder, encerrar, repetir,
  };
}

function tratarErro(
  e: unknown,
  setErro: (mensagem: string) => void,
  setErrosCampos: (campos: Record<string, string>) => void,
) {
  if (!axios.isAxiosError<ErroResponse>(e)) {
    setErro(ERRO_GENERICO);
    return;
  }
  if (e.code === 'ECONNABORTED' || e.code === 'ETIMEDOUT') {
    setErro(ERRO_TEMPO_ESGOTADO);
    return;
  }
  const dados = e.response?.data;
  if (e.response?.status === 503) {
    setErro(dados?.mensagem ?? ERRO_IA_INDISPONIVEL);
    return;
  }
  if (dados?.campos) {
    setErrosCampos(dados.campos);
  }
  setErro(dados?.mensagem ?? ERRO_GENERICO);
}
