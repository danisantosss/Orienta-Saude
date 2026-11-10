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

  it('mostra HTML digitado como texto, sem executar', () => {
    const { container } = render(<BalaoChat papel="USUARIO" conteudo={'<img src=x onerror="alert(1)"><script>alert(2)</script>'} />);

    expect(screen.getByText(/<script>alert\(2\)<\/script>/)).toBeInTheDocument();
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('script')).toBeNull();
  });
});
