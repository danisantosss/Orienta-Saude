import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { AxiosError, AxiosHeaders } from 'axios';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuth } from '../hooks/useAuth';
import { excluirConta, obterPerfil } from '../api/usuario';
import Perfil from './Perfil';

vi.mock('../hooks/useAuth');
vi.mock('../api/usuario');

const logout = vi.fn();

const PERFIL = {
  nome: 'Maria Silva',
  email: 'maria@exemplo.com',
  criadoEm: '2026-11-01T10:00:00',
  consentimentoEm: '2026-11-01T10:00:05',
  versaoPolitica: '1.0',
};

function PaginaLogin() {
  const location = useLocation();
  return <p>Login: {(location.state as { aviso?: string } | null)?.aviso}</p>;
}

function renderizar() {
  render(
    <MemoryRouter initialEntries={['/perfil']}>
      <Routes>
        <Route path="/perfil" element={<Perfil />} />
        <Route path="/login" element={<PaginaLogin />} />
      </Routes>
    </MemoryRouter>
  );
}

function erroDaApi(status: number, mensagem: string) {
  const headers = new AxiosHeaders();
  return new AxiosError('erro', String(status), { headers }, null, {
    status, statusText: '', headers, config: { headers }, data: { mensagem },
  });
}

describe('Perfil', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(useAuth).mockReturnValue({ logout } as unknown as ReturnType<typeof useAuth>);
  });

  it('mostra os dados da conta e a data do consentimento', async () => {
    vi.mocked(obterPerfil).mockResolvedValue(PERFIL);
    renderizar();

    expect(await screen.findByText('Maria Silva')).toBeInTheDocument();
    expect(screen.getByText('maria@exemplo.com')).toBeInTheDocument();
    expect(screen.getByText('01/11/2026')).toBeInTheDocument();
    expect(screen.getByText(/01\/11\/2026 às 10:00 \(versão 1\.0\)/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ler a Política de Privacidade' })).toHaveAttribute('href', '/privacidade');
  });

  it('avisa quando a conta foi criada antes do registro de consentimento', async () => {
    vi.mocked(obterPerfil).mockResolvedValue({ ...PERFIL, consentimentoEm: null, versaoPolitica: null });
    renderizar();

    expect(await screen.findByText('Não registrado (conta criada antes da política)')).toBeInTheDocument();
  });

  it('mostra erro quando o perfil não carrega', async () => {
    vi.mocked(obterPerfil).mockRejectedValue(new Error('rede'));
    renderizar();

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível carregar seus dados.');
  });

  it('pede a senha antes de excluir e permite cancelar', async () => {
    vi.mocked(obterPerfil).mockResolvedValue(PERFIL);
    renderizar();
    await userEvent.click(await screen.findByRole('button', { name: 'Excluir minha conta' }));

    await userEvent.click(screen.getByRole('button', { name: 'Confirmar exclusão' }));
    expect(screen.getByText('Informe sua senha para confirmar.')).toBeInTheDocument();
    expect(excluirConta).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(screen.queryByLabelText('Senha')).not.toBeInTheDocument();
  });

  it('mostra o erro da API quando a senha está errada', async () => {
    vi.mocked(obterPerfil).mockResolvedValue(PERFIL);
    vi.mocked(excluirConta).mockRejectedValue(erroDaApi(403, 'Senha incorreta'));
    renderizar();
    await userEvent.click(await screen.findByRole('button', { name: 'Excluir minha conta' }));
    await userEvent.type(screen.getByLabelText('Senha'), 'errada123');
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar exclusão' }));

    expect(await screen.findByText('Senha incorreta')).toBeInTheDocument();
    expect(logout).not.toHaveBeenCalled();
  });

  it('exclui a conta, sai e mostra a confirmação no login', async () => {
    vi.mocked(obterPerfil).mockResolvedValue(PERFIL);
    vi.mocked(excluirConta).mockResolvedValue('Recebemos sua solicitação. Sua conta e todos os seus dados foram excluídos.');
    renderizar();
    await userEvent.click(await screen.findByRole('button', { name: 'Excluir minha conta' }));
    await userEvent.type(screen.getByLabelText('Senha'), 'senhaSegura123');
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar exclusão' }));

    expect(excluirConta).toHaveBeenCalledWith('senhaSegura123');
    expect(logout).toHaveBeenCalled();
    expect(await screen.findByText(/Sua conta e todos os seus dados foram excluídos/)).toBeInTheDocument();
  });
});
