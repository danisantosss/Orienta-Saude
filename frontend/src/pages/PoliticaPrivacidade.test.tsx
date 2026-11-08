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
});
