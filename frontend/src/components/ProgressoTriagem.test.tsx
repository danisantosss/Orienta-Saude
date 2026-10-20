import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import ProgressoTriagem from './ProgressoTriagem';

describe('ProgressoTriagem', () => {
  it('marca a etapa atual para leitores de tela', () => {
    render(<ProgressoTriagem etapaAtual={2} />);

    const etapas = screen.getAllByRole('listitem');
    expect(etapas).toHaveLength(3);
    expect(etapas[1]).toHaveAttribute('aria-current', 'step');
    expect(etapas[0]).not.toHaveAttribute('aria-current');
    expect(etapas[0]).toHaveClass('etapa-concluida');
  });
});
