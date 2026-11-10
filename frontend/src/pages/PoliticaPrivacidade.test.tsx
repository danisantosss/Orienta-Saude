import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import PoliticaPrivacidade, { VERSAO_POLITICA } from './PoliticaPrivacidade';

describe('PoliticaPrivacidade', () => {
  it('mostra a versão e as seções principais da política e dos termos', () => {
    render(<MemoryRouter><PoliticaPrivacidade /></MemoryRouter>);

    expect(screen.getByRole('heading', { level: 1, name: 'Política de Privacidade e Termos de Uso' })).toBeInTheDocument();
    expect(screen.getByText(new RegExp(`Versão ${VERSAO_POLITICA.replace('.', '\\.')}`))).toBeInTheDocument();
    for (const secao of ['Quais dados coletamos', 'Uso de inteligência artificial', 'Seus direitos', 'Termos de uso']) {
      expect(screen.getByRole('heading', { name: secao })).toBeInTheDocument();
    }
  });

  it('informa que o texto anonimizado é processado pelo Google e não envia nome nem email', () => {
    render(<MemoryRouter><PoliticaPrivacidade /></MemoryRouter>);

    expect(screen.getByText(/processado pelo Google/)).toBeInTheDocument();
    expect(screen.getByText(/nunca seu nome, seu email/)).toBeInTheDocument();
  });

  it('informa onde os dados ficam guardados e que isso é uma transferência internacional', () => {
    render(<MemoryRouter><PoliticaPrivacidade /></MemoryRouter>);

    expect(screen.getByRole('heading', { name: 'Onde os dados ficam guardados' })).toBeInTheDocument();
    expect(screen.getByText(/Microsoft Azure, na região do México/)).toBeInTheDocument();
    expect(screen.getByText(/transferência internacional de dados/)).toBeInTheDocument();
  });

  it('lista as mudanças de cada versão no fim da página', () => {
    render(<MemoryRouter><PoliticaPrivacidade /></MemoryRouter>);

    expect(screen.getByRole('heading', { name: 'Alterações nesta política' })).toBeInTheDocument();
    expect(screen.getByText(/1\.1 \(10\/11\/2026\): informa onde os dados ficam guardados/)).toBeInTheDocument();
  });
});
