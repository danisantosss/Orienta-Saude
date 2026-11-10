import { Link } from 'react-router-dom';

/** Mesma versão gravada no backend quando o usuário aceita a política. Mudou o texto, muda a versão. */
export const VERSAO_POLITICA = '1.1';

/**
 * Política de Privacidade e Termos de Uso (rota pública /privacidade).
 * Fica acessível antes do cadastro, pelo link do formulário, e a qualquer momento pelo rodapé.
 */
export default function PoliticaPrivacidade() {
  return (
    <article className="pagina-politica cartao">
      <h1>Política de Privacidade e Termos de Uso</h1>
      <p className="texto-secundario">Versão {VERSAO_POLITICA}, de 10 de novembro de 2026.</p>

      <p>
        O Orienta Saúde é um projeto acadêmico (Trabalho de Conclusão de Curso) que faz uma triagem
        educativa de sintomas. Esta página explica quais dados usamos, por que e quais são os seus
        direitos pela Lei Geral de Proteção de Dados (LGPD, Lei 13.709/2018).
      </p>

      <h2>Quais dados coletamos</h2>
      <ul>
        <li><strong>Cadastro:</strong> nome, email e senha. A senha é guardada só como hash (BCrypt), nunca em texto.</li>
        <li><strong>Consentimento:</strong> a data em que você aceitou esta política e a versão aceita.</li>
        <li>
          <strong>Triagem:</strong> idade, sintomas, respostas da entrevista e o resultado. São dados de saúde,
          que a LGPD trata como dados pessoais sensíveis (art. 5º, II).
        </li>
      </ul>
      <p>Não coletamos CPF, endereço, telefone, localização nem dados de navegação para publicidade.</p>

      <h2>Para que usamos</h2>
      <p>
        Os dados de saúde são usados só para fazer a sua triagem e mostrar o seu histórico. A base legal é o
        seu consentimento específico e destacado (LGPD, art. 11, I), dado na caixa de aceite do cadastro.
        Não vendemos nem compartilhamos seus dados com ninguém além do descrito abaixo.
      </p>

      <h2>Uso de inteligência artificial</h2>
      <p>
        As perguntas da entrevista e a análise final são geradas pela API Gemini, do Google. Para isso enviamos
        apenas a idade, os sintomas e as respostas da entrevista, <strong>nunca seu nome, seu email ou qualquer
        identificador da sua conta</strong>. Esse texto anonimizado é processado pelo Google.
      </p>
      <p>
        O projeto usa o plano gratuito da API, em que o Google pode usar o conteúdo enviado para melhorar os seus
        produtos. Por isso, evite escrever nos sintomas seu nome, documentos ou qualquer dado que identifique você
        ou outra pessoa.
      </p>

      <h2>Como protegemos seus dados</h2>
      <ul>
        <li>Toda a comunicação usa HTTPS.</li>
        <li>Sintomas, mensagens e resultados ficam criptografados no banco de dados (AES-256).</li>
        <li>Cada pessoa só acessa as próprias triagens, com login por token que expira em 24 horas.</li>
      </ul>

      <h2>Onde os dados ficam guardados</h2>
      <p>
        O site e o banco de dados ficam na Microsoft Azure, na região do México. Como os servidores estão fora do
        Brasil, isso é uma transferência internacional de dados (LGPD, art. 33), feita para um provedor que aplica
        criptografia e controle de acesso. Os textos enviados à IA são processados pelo Google, também fora do Brasil.
      </p>

      <h2>Por quanto tempo guardamos</h2>
      <p>Enquanto a sua conta existir. Ao excluir a conta, todos os dados são apagados na hora.</p>

      <h2>Seus direitos</h2>
      <ul>
        <li><strong>Consultar seus dados:</strong> na página <Link to="/perfil">Meu perfil</Link> e no histórico.</li>
        <li>
          <strong>Excluir a conta e revogar o consentimento:</strong> em Meu perfil, com o botão "Excluir minha conta".
          A conta, as triagens, as mensagens e os resultados são apagados, e o login deixa de funcionar.
          Fica registrado apenas que uma exclusão aconteceu, sem nome, email ou dado de saúde.
        </li>
        <li>
          <strong>Dúvidas ou outros pedidos:</strong> abra uma solicitação no{' '}
          <a href="https://github.com/danisantosss/Orienta-Saude/issues" target="_blank" rel="noopener noreferrer">
            repositório do projeto
          </a>, sem colocar dados de saúde na mensagem.
        </li>
      </ul>

      <h2>Termos de uso</h2>
      <ul>
        <li>
          O Orienta Saúde é <strong>educativo</strong>: não faz diagnóstico, não prescreve tratamento e não substitui
          a avaliação de um profissional de saúde.
        </li>
        <li>Em caso de emergência, ligue para o <strong>SAMU (192)</strong> ou procure o pronto-socorro mais próximo.</li>
        <li>
          A conta é pessoal. Se você fizer a triagem por outra pessoa (por exemplo, um filho), faça isso como responsável
          por ela.
        </li>
        <li>Não use o serviço para enviar conteúdo ofensivo ou tentar acessar dados de outras pessoas.</li>
      </ul>

      <h2>Alterações nesta política</h2>
      <p>
        Se o texto mudar, a versão muda junto e a mudança fica listada aqui. Esta é a versão {VERSAO_POLITICA}.
      </p>
      <ul>
        <li>1.1 (10/11/2026): informa onde os dados ficam guardados.</li>
        <li>1.0 (06/11/2026): primeira versão.</li>
      </ul>
    </article>
  );
}
