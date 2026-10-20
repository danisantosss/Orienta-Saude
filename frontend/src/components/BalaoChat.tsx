import type { MensagemChat } from '../hooks/useTriagem';

/** Uma mensagem da entrevista: pergunta da IA à esquerda, resposta do usuário à direita. */
export default function BalaoChat({ papel, conteudo }: MensagemChat) {
  const daIa = papel === 'ASSISTENTE';
  return (
    <li className={`balao ${daIa ? 'balao-assistente' : 'balao-usuario'}`}>
      <span className="somente-leitor">{daIa ? 'Orienta Saúde:' : 'Você:'}</span> {conteudo}
    </li>
  );
}
