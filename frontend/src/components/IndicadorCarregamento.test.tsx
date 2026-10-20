import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import IndicadorCarregamento from './IndicadorCarregamento';

describe('IndicadorCarregamento', () => {
  it('anuncia a mensagem de carregamento', () => {
    render(<IndicadorCarregamento mensagem="Analisando seus sintomas..." />);

    expect(screen.getByRole('status')).toHaveTextContent('Analisando seus sintomas...');
  });

  it('avisa quando está demorando mais que o esperado', () => {
    render(<IndicadorCarregamento mensagem="Analisando..." demorando />);

    expect(screen.getByRole('status')).toHaveTextContent('está demorando mais que o esperado');
  });
});
