import { apiClient } from './client';
import type { IndicadoresResponse } from '../types';

export interface FiltroIndicadores {
  inicio?: string;
  fim?: string;
}

/** Indicadores agregados do painel. Sem datas, o backend usa os últimos 30 dias. */
export async function obterIndicadores(filtro: FiltroIndicadores = {}): Promise<IndicadoresResponse> {
  const params: Record<string, string> = {};
  if (filtro.inicio) params.inicio = filtro.inicio;
  if (filtro.fim) params.fim = filtro.fim;
  const resposta = await apiClient.get<IndicadoresResponse>('/api/admin/indicadores', { params });
  return resposta.data;
}
