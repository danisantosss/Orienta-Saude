import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import BalaoChat from './BalaoChat';

describe('BalaoChat', () => {
  it('identifica quem falou para leitores de tela', () => {
    render(
      <>
        <BalaoChat papel="ASSISTENTE" conteudo="Há quanto tempo?" />
        <BalaoChat papel="USUARIO" conteudo="Dois dias" />
      </>
    );

    expect(screen.getByText('Há quanto tempo?').closest('li')).toHaveClass('balao-assistente');
    expect(screen.getByText('Dois dias').closest('li')).toHaveClass('balao-usuario');
    expect(screen.getByText('Orienta Saúde:')).toHaveClass('somente-leitor');
    expect(screen.getByText('Você:')).toHaveClass('somente-leitor');
  });
});
