import { apiClient } from './client';
import type {
  FiltroHistorico,
  IniciarTriagemRequest,
  IniciarTriagemResponse,
  MensagemTriagemResponse,
  Pagina,
  ResultadoTriagemResponse,
  SessaoTriagemResponse,
} from '../types';

/** Cada chamada da triagem é cancelada depois de 30 s. */
export const LIMITE_REQUISICAO_MS = 30000;

const opcoes = { timeout: LIMITE_REQUISICAO_MS };

export async function iniciar(dados: IniciarTriagemRequest): Promise<IniciarTriagemResponse> {
  const resposta = await apiClient.post<IniciarTriagemResponse>('/api/triagem/iniciar', dados, opcoes);
  return resposta.data;
}

export async function enviarMensagem(sessaoId: number, conteudo: string): Promise<MensagemTriagemResponse> {
  const resposta = await apiClient.post<MensagemTriagemResponse>(`/api/triagem/${sessaoId}/mensagem`, { conteudo }, opcoes);
  return resposta.data;
}

export async function encerrar(sessaoId: number): Promise<MensagemTriagemResponse> {
  const resposta = await apiClient.post<MensagemTriagemResponse>(`/api/triagem/${sessaoId}/encerrar`, null, opcoes);
  return resposta.data;
}

export async function analisar(sessaoId: number): Promise<ResultadoTriagemResponse> {
  const resposta = await apiClient.post<ResultadoTriagemResponse>(`/api/triagem/${sessaoId}/analisar`, null, opcoes);
  return resposta.data;
}

export async function obterResultado(sessaoId: number): Promise<ResultadoTriagemResponse> {
  const resposta = await apiClient.get<ResultadoTriagemResponse>(`/api/triagem/${sessaoId}/resultado`);
  return resposta.data;
}

export async function listarHistorico(filtro: FiltroHistorico = {}): Promise<Pagina<SessaoTriagemResponse>> {
  const resposta = await apiClient.get<Pagina<SessaoTriagemResponse>>('/api/triagem/historico', { params: filtro });
  return resposta.data;
}
