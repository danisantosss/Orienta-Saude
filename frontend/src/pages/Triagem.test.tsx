import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useTriagem } from '../hooks/useTriagem';
import Triagem from './Triagem';

vi.mock('../hooks/useTriagem');

type EstadoTriagem = ReturnType<typeof useTriagem>;

const acoes = {
  iniciar: vi.fn(),
  responder: vi.fn(),
  encerrar: vi.fn(),
  repetir: vi.fn(),
};

function comEstado(estado: Partial<EstadoTriagem> = {}) {
  vi.mocked(useTriagem).mockReturnValue({
    sessaoId: null, mensagens: [], carregando: false, demorando: false, completa: false, concluida: false,
    aviso: null, erro: null, errosCampos: {}, ...acoes, ...estado,
  });
}

function renderizar() {
  render(
    <MemoryRouter initialEntries={['/triagem']}>
      <Routes>
        <Route path="/triagem" element={<Triagem />} />
        <Route path="/resultado/:id" element={<p>Página de resultado</p>} />
      </Routes>
    </MemoryRouter>
  );
}

async function confirmarAviso() {
  await userEvent.click(screen.getByRole('checkbox', { name: /li e entendi/i }));
}

describe('Triagem', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    comEstado();
  });

  it('mostra o aviso no topo e só libera o envio depois da confirmação', async () => {
    renderizar();

    const aviso = screen.getByRole('note');
    const formulario = screen.getByRole('form', { name: 'Sintomas' });
    expect(aviso.compareDocumentPosition(formulario) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(screen.getByRole('button', { name: /continuar/i })).toBeDisabled();

    await confirmarAviso();

    expect(screen.getByRole('button', { name: /continuar/i })).toBeEnabled();
  });

  it('valida os campos sem apagar o que foi digitado', async () => {
    renderizar();
    await confirmarAviso();
    await userEvent.type(screen.getByLabelText('Ou descreva com suas palavras'), 'febre');
    await userEvent.type(screen.getByLabelText('Idade'), '130');
    await userEvent.click(screen.getByRole('button', { name: /continuar/i }));

    expect(screen.getByText(/pelo menos 10 caracteres/)).toBeInTheDocument();
    expect(screen.getByText('Informe uma idade entre 1 e 120 anos.')).toBeInTheDocument();
    expect(screen.getByLabelText('Ou descreva com suas palavras')).toHaveValue('febre');
    expect(screen.getByLabelText('Idade')).toHaveValue(130);
    expect(acoes.iniciar).not.toHaveBeenCalled();
  });

  it('junta os sintomas escolhidos com a descrição livre', async () => {
    renderizar();
    await confirmarAviso();
    await userEvent.click(screen.getByRole('button', { name: 'Febre' }));
    await userEvent.click(screen.getByRole('button', { name: 'Tosse' }));
    expect(screen.getByRole('button', { name: 'Febre' })).toHaveAttribute('aria-pressed', 'true');
    await userEvent.type(screen.getByLabelText('Ou descreva com suas palavras'), 'Desde ontem');
    await userEvent.type(screen.getByLabelText('Idade'), '30');
    await userEvent.click(screen.getByRole('button', { name: /continuar/i }));

    expect(acoes.iniciar).toHaveBeenCalledWith({ sintomas: 'Estou com febre, tosse. Desde ontem', idade: 30 });
  });

  it('mostra os erros de validação do servidor perto do campo', () => {
    comEstado({ errosCampos: { idade: 'A idade deve estar entre 1 e 120' }, erro: 'Erro de validação' });
    renderizar();

    expect(screen.getByText('A idade deve estar entre 1 e 120')).toBeInTheDocument();
  });

  it('na entrevista mostra o chat, envia a resposta e permite encerrar', async () => {
    comEstado({ sessaoId: 7, mensagens: [{ papel: 'ASSISTENTE', conteudo: 'Há quanto tempo?' }] });
    renderizar();

    expect(screen.getByText('Há quanto tempo?')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Sua resposta'), 'Dois dias');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));
    expect(acoes.responder).toHaveBeenCalledWith('Dois dias');
    expect(screen.getByLabelText('Sua resposta')).toHaveValue('');

    await userEvent.click(screen.getByRole('button', { name: /encerrar entrevista/i }));
    expect(acoes.encerrar).toHaveBeenCalled();
  });

  it('em caso de erro mostra a mensagem e o botão de tentar de novo', async () => {
    comEstado({ sessaoId: 7, mensagens: [{ papel: 'ASSISTENTE', conteudo: 'Há quanto tempo?' }], erro: 'Serviço indisponível.' });
    renderizar();

    expect(screen.getByRole('alert')).toHaveTextContent('Serviço indisponível.');
    await userEvent.click(screen.getByRole('button', { name: /tentar novamente/i }));
    expect(acoes.repetir).toHaveBeenCalled();
  });

  it('durante a análise mostra o carregamento e o aviso de demora', () => {
    comEstado({ sessaoId: 7, completa: true, carregando: true, demorando: true, aviso: 'Coleta inconclusiva.' });
    renderizar();

    expect(screen.getByText('Analisando suas respostas...')).toBeInTheDocument();
    expect(screen.getByText(/demorando mais que o esperado/)).toBeInTheDocument();
    expect(screen.getByText('Coleta inconclusiva.')).toBeInTheDocument();
    expect(screen.queryByLabelText('Sua resposta')).not.toBeInTheDocument();
  });

  it('quando o resultado fica pronto, abre a página do resultado', async () => {
    comEstado({ sessaoId: 7, completa: true, concluida: true });
    renderizar();

    expect(await screen.findByText('Página de resultado')).toBeInTheDocument();
  });
});
