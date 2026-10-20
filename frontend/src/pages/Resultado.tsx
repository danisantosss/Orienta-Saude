import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import axios from 'axios';
import { obterResultado } from '../api/triagem';
import BadgeUrgencia from '../components/BadgeUrgencia';
import Disclaimer from '../components/Disclaimer';
import IndicadorCarregamento from '../components/IndicadorCarregamento';
import Toast from '../components/Toast';
import type { NivelUrgencia, ResultadoTriagemResponse } from '../types';

/** O que fazer em cada nível (Requisitos 7.1 a 7.4). */
export const RECOMENDACOES: Record<NivelUrgencia, string> = {
  EMERGENCIA: 'Procure atendimento médico presencial imediatamente: vá ao pronto-socorro mais próximo ou ligue para o SAMU (192).',
  URGENTE: 'Procure atendimento médico em até 24 horas.',
  MODERADO: 'Agende uma consulta médica em até 7 dias e acompanhe os sintomas. Se aparecer algum sinal de alerta, procure atendimento antes.',
  LEVE: 'Os sintomas podem ser acompanhados com cuidados em casa. Fique atento aos sinais de alerta e procure atendimento se eles aparecerem.',
};

function mensagemDeErro(erro: unknown): string {
  const status = axios.isAxiosError(erro) ? erro.response?.status : undefined;
  if (status === 404) return 'Resultado não encontrado.';
  if (status === 422) return 'Esta triagem ainda não tem resultado.';
  return 'Não foi possível carregar o resultado. Tente novamente.';
}

function formatarData(data: string | null): string | null {
  if (!data) return null;
  return new Date(data).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

/**
 * Página de resultado (rota /resultado/:id). Emergência vem antes de tudo e o aviso legal
 * é sempre o último bloco (Requisitos 7.1 e 14.3).
 */
export default function Resultado() {
  const { id } = useParams();
  const location = useLocation();
  const [resultado, setResultado] = useState<ResultadoTriagemResponse | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [mostrarToast, setMostrarToast] = useState(Boolean((location.state as { novaTriagem?: boolean } | null)?.novaTriagem));
  const fecharToast = useCallback(() => setMostrarToast(false), []);

  useEffect(() => {
    let ativo = true;
    obterResultado(Number(id))
      .then((dados) => { if (ativo) setResultado(dados); })
      .catch((e) => { if (ativo) setErro(mensagemDeErro(e)); });
    return () => { ativo = false; };
  }, [id]);

  if (erro) {
    return (
      <div className="pagina-resultado">
        <p role="alert" className="form-error">{erro}</p>
        <Link to="/triagem">Fazer uma nova triagem</Link>
      </div>
    );
  }

  if (!resultado) {
    return <IndicadorCarregamento mensagem="Carregando o resultado..." />;
  }

  const emergencia = resultado.nivelUrgencia === 'EMERGENCIA';
  const concluidaEm = formatarData(resultado.concluidaEm);

  return (
    <div className="pagina-resultado">
      {emergencia && (
        <div role="alert" className="alerta-emergencia">
          <strong>Atenção: isto pode ser uma emergência.</strong>
          <p>{RECOMENDACOES.EMERGENCIA}</p>
        </div>
      )}

      <div className="grade-resultado">
        <aside className="cartao resumo-triagem" aria-label="Resumo da sua triagem">
          <h2>Resumo da sua triagem</h2>
          {concluidaEm && <p className="texto-secundario">Realizada em {concluidaEm}</p>}
          <h3>Sintomas informados</h3>
          <p>{resultado.sintomasIniciais}</p>
          <h3>Idade</h3>
          <p>{resultado.idadePaciente} anos</p>
          {resultado.perguntasERespostas.length > 0 && (
            <details>
              <summary>Perguntas e respostas</summary>
              <dl>
                {resultado.perguntasERespostas.map((item, indice) => (
                  <div key={indice}>
                    <dt>{item.pergunta}</dt>
                    <dd>{item.resposta ?? 'Sem resposta (entrevista encerrada antes)'}</dd>
                  </div>
                ))}
              </dl>
            </details>
          )}
        </aside>

        <section className="conteudo-resultado">
          <h1>Resultado da análise</h1>
          <p>
            Com base nos sintomas informados, a IA analisou suas respostas e consultou a base de conhecimento.
            As informações abaixo <strong>não representam um diagnóstico</strong>.
          </p>

          <div className={`cartao recomendacao recomendacao-${resultado.nivelUrgencia.toLowerCase()}`}>
            <BadgeUrgencia nivel={resultado.nivelUrgencia} />
            {!emergencia && <p>{RECOMENDACOES[resultado.nivelUrgencia]}</p>}
            <p>{resultado.quandoBuscarAtendimento}</p>
          </div>

          {resultado.notaLimitacao && <p className="aviso-limitacao">{resultado.notaLimitacao}</p>}

          <section className="cartao destaque-lilas" aria-labelledby="titulo-condicoes">
            <h2 id="titulo-condicoes">Possíveis condições relacionadas</h2>
            <p className="texto-secundario">Os sintomas podem estar relacionados a estas condições. Só um profissional de saúde pode confirmar.</p>
            <ul className="lista-condicoes">
              {resultado.condicoes.map((condicao) => <li key={condicao}>{condicao}</li>)}
            </ul>
            <p>{resultado.explicacaoEducativa}</p>
          </section>
        </section>

        <aside className="coluna-orientacoes">
          <section className="cartao destaque-lilas" aria-labelledby="titulo-orientacoes">
            <h2 id="titulo-orientacoes">Orientações recomendadas</h2>
            <p>{resultado.orientacoesGerais}</p>
          </section>
          <section className="cartao sinais-alerta" aria-labelledby="titulo-sinais">
            <h2 id="titulo-sinais">Procure atendimento se aparecer</h2>
            <ul>
              {resultado.sinaisAlerta.map((sinal) => <li key={sinal}>{sinal}</li>)}
            </ul>
          </section>
          {resultado.fontes.length > 0 && (
            <section className="cartao" aria-labelledby="titulo-fontes">
              <h2 id="titulo-fontes">Fontes consultadas</h2>
              <ul>
                {resultado.fontes.map((fonte) => (
                  <li key={fonte.url + fonte.sintoma}>
                    <a href={fonte.url} target="_blank" rel="noopener noreferrer">{fonte.fonte}</a> ({fonte.sintoma})
                  </li>
                ))}
              </ul>
            </section>
          )}
        </aside>
      </div>

      {mostrarToast && <Toast mensagem="Triagem concluída." aoFechar={fecharToast} />}
      <Disclaimer texto={resultado.disclaimer} />
    </div>
  );
}
