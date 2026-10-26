import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import * as triagemApi from '../api/triagem';
import type { Pagina, SessaoTriagemResponse } from '../types';
import Historico from './Historico';

vi.mock('../api/triagem');

function sessao(dados: Partial<SessaoTriagemResponse> = {}): SessaoTriagemResponse {
  return {
    id: 5,
    sintomasIniciais: 'Febre, dor de cabeça, tosse, dor no corpo',
    idadePaciente: 30,
    status: 'CONCLUIDA',
    iniciadaEm: '2026-06-12T17:30:00',
    finalizadaEm: '2026-06-12T17:35:00',
    nivelUrgencia: 'LEVE',
    ...dados,
  };
}

function pagina(itens: SessaoTriagemResponse[], dados: Partial<Pagina<SessaoTriagemResponse>> = {}): Pagina<SessaoTriagemResponse> {
  return { itens, pagina: 0, tamanho: 20, totalItens: itens.length, totalPaginas: itens.length > 0 ? 1 : 0, ...dados };
}

function renderizar() {
  render(
    <MemoryRouter initialEntries={['/historico']}>
      <Routes>
        <Route path="/historico" element={<Historico />} />
        <Route path="/resultado/:id" element={<p>Página de resultado</p>} />
        <Route path="/triagem" element={<p>Página de triagem</p>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('Historico', () => {
  beforeEach(() => vi.resetAllMocks());

  it('lista as triagens com data, sintomas e nível de urgência', async () => {
    vi.mocked(triagemApi.listarHistorico).mockResolvedValue(pagina([
      sessao(),
      sessao({ id: 4, sintomasIniciais: 'Náusea, diarreia', nivelUrgencia: 'URGENTE', iniciadaEm: '2026-04-17T09:30:00' }),
    ]));
    renderizar();

    expect(screen.getByRole('status')).toHaveTextContent('Carregando');
    const itens = await screen.findAllByRole('listitem');
    expect(itens).toHaveLength(2);
    expect(within(itens[0]).getByText('12/06/2026 às 17:30')).toBeInTheDocument();
    expect(within(itens[0]).getByText('Febre, dor de cabeça, tosse, dor no corpo')).toBeInTheDocument();
    expect(within(itens[0]).getByLabelText('Nível de urgência: Leve')).toBeInTheDocument();
    expect(within(itens[1]).getByLabelText('Nível de urgência: Urgente')).toBeInTheDocument();
    expect(triagemApi.listarHistorico).toHaveBeenCalledWith({ pagina: 0 });
  });

  it('abre o resultado completo da triagem escolhida', async () => {
    vi.mocked(triagemApi.listarHistorico).mockResolvedValue(pagina([sessao()]));
    renderizar();

    await userEvent.click(await screen.findByRole('link', { name: /Ver detalhes/ }));

    expect(screen.getByText('Página de resultado')).toBeInTheDocument();
  });

  it('triagem não concluída aparece sem nível e sem link para o resultado', async () => {
    vi.mocked(triagemApi.listarHistorico).mockResolvedValue(pagina([
      sessao({ status: 'EM_PROGRESSO', nivelUrgencia: null, finalizadaEm: null }),
    ]));
    renderizar();

    expect(await screen.findByText('Não concluída')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Ver detalhes/ })).not.toBeInTheDocument();
  });

  it('corta sintomas muito longos na listagem', async () => {
    vi.mocked(triagemApi.listarHistorico).mockResolvedValue(pagina([sessao({ sintomasIniciais: 'a'.repeat(300) })]));
    renderizar();

    const item = await screen.findByRole('listitem');
    expect(within(item).getByText(`${'a'.repeat(120)}…`)).toBeInTheDocument();
  });

  it('sem nenhuma triagem, orienta a fazer a primeira', async () => {
    vi.mocked(triagemApi.listarHistorico).mockResolvedValue(pagina([]));
    renderizar();

    expect(await screen.findByText('Você ainda não fez nenhuma triagem.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('link', { name: 'Fazer minha primeira triagem' }));
    expect(screen.getByText('Página de triagem')).toBeInTheDocument();
  });

  it('filtra por nível e por período e volta para a primeira página', async () => {
    vi.mocked(triagemApi.listarHistorico).mockResolvedValue(pagina([sessao()]));
    renderizar();
    await screen.findByRole('listitem');

    await userEvent.selectOptions(screen.getByLabelText('Nível de urgência'), 'URGENTE');
    expect(triagemApi.listarHistorico).toHaveBeenLastCalledWith({ pagina: 0, nivelUrgencia: 'URGENTE' });

    fireEvent.change(screen.getByLabelText('De'), { target: { value: '2026-04-01' } });
    fireEvent.change(screen.getByLabelText('Até'), { target: { value: '2026-04-30' } });
    expect(triagemApi.listarHistorico).toHaveBeenLastCalledWith({
      pagina: 0, nivelUrgencia: 'URGENTE', inicio: '2026-04-01', fim: '2026-04-30',
    });
  });

  it('com filtros e nenhum resultado, mostra outra mensagem e permite limpar', async () => {
    vi.mocked(triagemApi.listarHistorico).mockResolvedValue(pagina([]));
    renderizar();
    await screen.findByText('Você ainda não fez nenhuma triagem.');

    await userEvent.selectOptions(screen.getByLabelText('Nível de urgência'), 'EMERGENCIA');
    expect(await screen.findByText('Nenhuma triagem encontrada com esses filtros.')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Limpar filtros' }));
    expect(screen.getByLabelText('Nível de urgência')).toHaveValue('');
    expect(triagemApi.listarHistorico).toHaveBeenLastCalledWith({ pagina: 0 });
  });

  it('navega entre as páginas', async () => {
    vi.mocked(triagemApi.listarHistorico)
      .mockResolvedValueOnce(pagina([sessao()], { totalItens: 25, totalPaginas: 2 }))
      .mockResolvedValueOnce(pagina([sessao({ id: 1 })], { pagina: 1, totalItens: 25, totalPaginas: 2 }));
    renderizar();

    expect(await screen.findByText('Página 1 de 2')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled();

    await userEvent.click(screen.getByRole('button', { name: 'Próxima página' }));

    expect(await screen.findByText('Página 2 de 2')).toBeInTheDocument();
    expect(triagemApi.listarHistorico).toHaveBeenLastCalledWith({ pagina: 1 });
    expect(screen.getByRole('button', { name: 'Próxima página' })).toBeDisabled();
  });

  it('mostra o erro e permite tentar de novo', async () => {
    vi.mocked(triagemApi.listarHistorico)
      .mockRejectedValueOnce(new Error('falha'))
      .mockResolvedValueOnce(pagina([sessao()]));
    renderizar();

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível carregar o histórico.');
    await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }));

    expect(await screen.findByRole('listitem')).toBeInTheDocument();
    expect(triagemApi.listarHistorico).toHaveBeenCalledTimes(2);
  });
});
