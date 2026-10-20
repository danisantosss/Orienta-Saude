import { render, screen, within } from '@testing-library/react';
import { AxiosError, AxiosHeaders } from 'axios';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import * as triagemApi from '../api/triagem';
import type { NivelUrgencia, ResultadoTriagemResponse } from '../types';
import Resultado from './Resultado';

vi.mock('../api/triagem');

function resultadoCom(dados: Partial<ResultadoTriagemResponse> = {}): ResultadoTriagemResponse {
  return {
    sessaoId: 7,
    sintomasIniciais: 'Estou com febre, tosse',
    idadePaciente: 30,
    nivelUrgencia: 'MODERADO',
    condicoes: ['Gripe', 'Resfriado comum'],
    explicacaoEducativa: 'Os sintomas podem estar relacionados a uma infecção viral.',
    orientacoesGerais: 'Beba bastante água e descanse.',
    sinaisAlerta: ['Falta de ar', 'Febre acima de 39 °C por mais de 3 dias'],
    quandoBuscarAtendimento: 'Procure uma unidade básica de saúde se não melhorar.',
    notaLimitacao: null,
    disclaimer: 'Estas orientações são exclusivamente educativas.',
    fontes: [{ sintoma: 'febre', fonte: 'Ministério da Saúde', url: 'https://www.gov.br/saude' }],
    perguntasERespostas: [{ pergunta: 'Há quanto tempo?', resposta: 'Dois dias' }],
    concluidaEm: '2026-10-19T10:30:00',
    ...dados,
  };
}

function erroHttp(status: number): AxiosError {
  return new AxiosError('erro', 'ERR_BAD_REQUEST', undefined, undefined, {
    status, data: {}, statusText: '', headers: {}, config: { headers: new AxiosHeaders() },
  });
}

function renderizar(estado?: unknown) {
  render(
    <MemoryRouter initialEntries={[{ pathname: '/resultado/7', state: estado }]}>
      <Routes>
        <Route path="/resultado/:id" element={<Resultado />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('Resultado', () => {
  beforeEach(() => vi.resetAllMocks());

  it('mostra a classificação, as condições, as orientações e as fontes', async () => {
    vi.mocked(triagemApi.obterResultado).mockResolvedValue(resultadoCom());
    renderizar();

    expect(screen.getByRole('status')).toHaveTextContent('Carregando');
    expect(await screen.findByLabelText('Nível de urgência: Moderado')).toBeInTheDocument();
    expect(triagemApi.obterResultado).toHaveBeenCalledWith(7);
    const condicoes = screen.getByRole('region', { name: 'Possíveis condições relacionadas' });
    expect(within(condicoes).getAllByRole('listitem')).toHaveLength(2);
    expect(screen.getByText('Beba bastante água e descanse.')).toBeInTheDocument();
    expect(screen.getByText('Falta de ar')).toBeInTheDocument();
    expect(screen.getByText('Estou com febre, tosse')).toBeInTheDocument();
    const fonte = screen.getByRole('link', { name: /Ministério da Saúde/ });
    expect(fonte).toHaveAttribute('href', 'https://www.gov.br/saude');
    expect(fonte).toHaveAttribute('rel', 'noopener noreferrer');
    expect(screen.getByText('Há quanto tempo?')).toBeInTheDocument();
  });

  it('emergência aparece antes de qualquer outro conteúdo, com o SAMU', async () => {
    vi.mocked(triagemApi.obterResultado).mockResolvedValue(resultadoCom({ nivelUrgencia: 'EMERGENCIA' }));
    renderizar();

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('192');
    const pagina = alerta.parentElement!;
    expect(pagina.firstElementChild).toBe(alerta);
  });

  it.each<[NivelUrgencia, RegExp]>([
    ['URGENTE', /em até 24 horas/],
    ['MODERADO', /em até 7 dias/],
    ['LEVE', /cuidados em casa/],
  ])('recomendação do nível %s', async (nivel, texto) => {
    vi.mocked(triagemApi.obterResultado).mockResolvedValue(resultadoCom({ nivelUrgencia: nivel }));
    renderizar();

    expect(await screen.findByText(texto)).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('o aviso legal é o último bloco e a nota de limitação aparece', async () => {
    vi.mocked(triagemApi.obterResultado).mockResolvedValue(resultadoCom({ notaLimitacao: 'A entrevista foi encerrada antes.' }));
    renderizar();

    const aviso = await screen.findByRole('note');
    expect(aviso).toHaveTextContent('Estas orientações são exclusivamente educativas.');
    expect(aviso.parentElement!.lastElementChild).toBe(aviso);
    expect(screen.getByText('A entrevista foi encerrada antes.')).toBeInTheDocument();
  });

  it('depois de uma triagem nova mostra a mensagem de sucesso', async () => {
    vi.mocked(triagemApi.obterResultado).mockResolvedValue(resultadoCom());
    renderizar({ novaTriagem: true });

    expect(await screen.findByText('Triagem concluída.')).toBeInTheDocument();
  });

  it.each<[number, string]>([
    [404, 'Resultado não encontrado.'],
    [422, 'Esta triagem ainda não tem resultado.'],
    [500, 'Não foi possível carregar o resultado. Tente novamente.'],
  ])('erro %i mostra mensagem amigável', async (status, mensagem) => {
    vi.mocked(triagemApi.obterResultado).mockRejectedValue(erroHttp(status));
    renderizar();

    expect(await screen.findByRole('alert')).toHaveTextContent(mensagem);
  });
});
