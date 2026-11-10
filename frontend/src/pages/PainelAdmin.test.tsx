import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AxiosError, AxiosHeaders } from 'axios';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import * as adminApi from '../api/admin';
import type { IndicadoresResponse } from '../types';
import PainelAdmin from './PainelAdmin';

vi.mock('../api/admin');

function indicadores(dados: Partial<IndicadoresResponse> = {}): IndicadoresResponse {
  return {
    inicio: '2026-11-01',
    fim: '2026-11-03',
    agrupamento: 'DIA',
    totalTriagens: 5,
    triagensPorPeriodo: [
      { inicio: '2026-11-01', total: 3 },
      { inicio: '2026-11-02', total: 0 },
      { inicio: '2026-11-03', total: 2 },
    ],
    distribuicaoUrgencia: { LEVE: 2, MODERADO: 1, URGENTE: 1, EMERGENCIA: 0 },
    triagensFinalizadas: 4,
    encerradasAntecipadamente: 1,
    taxaEncerramentoAntecipado: 25,
    ...dados,
  };
}

function erroDaApi(mensagem: string) {
  return new AxiosError('erro', '400', undefined, undefined, {
    status: 400, statusText: 'Bad Request', headers: {}, config: { headers: new AxiosHeaders() },
    data: { mensagem },
  });
}

describe('PainelAdmin', () => {
  beforeEach(() => vi.resetAllMocks());

  it('mostra os números e os gráficos agregados', async () => {
    vi.mocked(adminApi.obterIndicadores).mockResolvedValue(indicadores());
    render(<PainelAdmin />);

    expect(await screen.findByText('Período: 01/11/2026 a 03/11/2026')).toBeInTheDocument();
    expect(screen.getByText('triagens iniciadas').previousSibling).toHaveTextContent('5');
    expect(screen.getByText('entrevistas finalizadas').previousSibling).toHaveTextContent('4');
    expect(screen.getByText(/encerradas antes da conclusão \(1\)/).previousSibling).toHaveTextContent('25%');

    const urgencia = screen.getByRole('region', { name: 'Distribuição por nível de urgência' });
    expect(within(urgencia).getByText('Leve').parentElement).toHaveTextContent('2 (50%)');
    expect(within(urgencia).getByText('Emergência').parentElement).toHaveTextContent('0 (0%)');

    const tabela = screen.getByRole('table', { name: 'Triagens por dia' });
    expect(within(tabela).getAllByRole('row')).toHaveLength(4);
    expect(within(tabela).getByText('01/11').nextSibling).toHaveTextContent('3');
  });

  it('agrupa os rótulos por mês em períodos longos', async () => {
    vi.mocked(adminApi.obterIndicadores).mockResolvedValue(indicadores({
      agrupamento: 'MES',
      triagensPorPeriodo: [{ inicio: '2026-09-01', total: 4 }, { inicio: '2026-10-01', total: 1 }],
    }));
    render(<PainelAdmin />);

    const tabela = await screen.findByRole('table', { name: 'Triagens por mês' });
    expect(within(tabela).getByText('set/26')).toBeInTheDocument();
  });

  it('busca de novo quando o período muda e volta aos últimos 30 dias', async () => {
    vi.mocked(adminApi.obterIndicadores).mockResolvedValue(indicadores());
    render(<PainelAdmin />);
    await screen.findByText(/Período:/);

    await userEvent.type(screen.getByLabelText('De'), '2026-10-01');

    await waitFor(() => expect(adminApi.obterIndicadores).toHaveBeenLastCalledWith({ inicio: '2026-10-01', fim: '' }));
    await userEvent.click(screen.getByRole('button', { name: 'Últimos 30 dias' }));
    await waitFor(() => expect(adminApi.obterIndicadores).toHaveBeenLastCalledWith({ inicio: '', fim: '' }));
  });

  it('avisa quando não há triagens no período', async () => {
    vi.mocked(adminApi.obterIndicadores).mockResolvedValue(indicadores({
      totalTriagens: 0, triagensPorPeriodo: [], triagensFinalizadas: 0, encerradasAntecipadamente: 0,
      taxaEncerramentoAntecipado: 0, distribuicaoUrgencia: { LEVE: 0, MODERADO: 0, URGENTE: 0, EMERGENCIA: 0 },
    }));
    render(<PainelAdmin />);

    expect(await screen.findByText('Nenhuma triagem neste período.')).toBeInTheDocument();
  });

  it('mostra a mensagem de erro do backend', async () => {
    vi.mocked(adminApi.obterIndicadores).mockRejectedValue(erroDaApi('O período pode ter no máximo 366 dias'));
    render(<PainelAdmin />);

    expect(await screen.findByRole('alert')).toHaveTextContent('O período pode ter no máximo 366 dias');
  });

  it('mostra uma mensagem genérica quando a falha não vem da API', async () => {
    vi.mocked(adminApi.obterIndicadores).mockRejectedValue(new Error('rede'));
    render(<PainelAdmin />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível carregar os indicadores.');
  });
});
