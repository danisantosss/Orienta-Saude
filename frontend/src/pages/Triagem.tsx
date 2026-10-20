import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import BalaoChat from '../components/BalaoChat';
import Disclaimer from '../components/Disclaimer';
import IndicadorCarregamento from '../components/IndicadorCarregamento';
import ProgressoTriagem from '../components/ProgressoTriagem';
import { useTriagem } from '../hooks/useTriagem';

export const SINTOMAS_COMUNS = [
  'Febre', 'Dor de cabeça', 'Tosse', 'Dor de garganta',
  'Dor de estômago', 'Dor nas articulações', 'Falta de ar', 'Náusea ou vômito',
];

const MAXIMO_CARACTERES = 1000;

/** Junta os sintomas escolhidos e a descrição livre num texto só, como o backend espera. */
export function montarSintomas(escolhidos: string[], descricao: string): string {
  const texto = descricao.trim();
  if (escolhidos.length === 0) return texto;
  const lista = `Estou com ${escolhidos.map((sintoma) => sintoma.toLowerCase()).join(', ')}`;
  return texto ? `${lista}. ${texto}` : lista;
}

/**
 * Página da triagem (rota /triagem): sintomas e idade, entrevista em formato de chat
 * e, no fim, a análise, que abre a página de resultado.
 */
export default function Triagem() {
  const triagem = useTriagem();
  const navigate = useNavigate();
  const [avisoConfirmado, setAvisoConfirmado] = useState(false);
  const [escolhidos, setEscolhidos] = useState<string[]>([]);
  const [descricao, setDescricao] = useState('');
  const [idade, setIdade] = useState('');
  const [errosLocais, setErrosLocais] = useState<{ sintomas?: string; idade?: string }>({});
  const [resposta, setResposta] = useState('');

  useEffect(() => {
    if (triagem.concluida && triagem.sessaoId !== null) {
      navigate(`/resultado/${triagem.sessaoId}`, { state: { novaTriagem: true } });
    }
  }, [triagem.concluida, triagem.sessaoId, navigate]);

  function alternarSintoma(sintoma: string) {
    setEscolhidos((atuais) => atuais.includes(sintoma)
      ? atuais.filter((item) => item !== sintoma)
      : [...atuais, sintoma]);
  }

  function enviarSintomas(e: FormEvent) {
    e.preventDefault();
    const sintomas = montarSintomas(escolhidos, descricao);
    const idadeNumero = Number(idade);
    const erros: { sintomas?: string; idade?: string } = {};
    if (sintomas.length < 10 || sintomas.length > MAXIMO_CARACTERES) {
      erros.sintomas = 'Escolha um sintoma ou descreva o que sente com pelo menos 10 caracteres.';
    }
    if (!Number.isInteger(idadeNumero) || idadeNumero < 1 || idadeNumero > 120) {
      erros.idade = 'Informe uma idade entre 1 e 120 anos.';
    }
    setErrosLocais(erros);
    if (Object.keys(erros).length === 0) {
      triagem.iniciar({ sintomas, idade: idadeNumero });
    }
  }

  function enviarResposta(e: FormEvent) {
    e.preventDefault();
    const conteudo = resposta.trim();
    if (!conteudo) return;
    setResposta('');
    triagem.responder(conteudo);
  }

  const emEntrevista = triagem.sessaoId !== null;
  const erroSintomas = errosLocais.sintomas ?? triagem.errosCampos.sintomas;
  const erroIdade = errosLocais.idade ?? triagem.errosCampos.idade;

  const blocoErro = triagem.erro && (
    <div role="alert" className="form-error">
      <p>{triagem.erro}</p>
      {emEntrevista && (
        <button type="button" className="botao-secundario" onClick={() => triagem.repetir()}>Tentar novamente</button>
      )}
    </div>
  );

  return (
    <div className="pagina-triagem">
      <ProgressoTriagem etapaAtual={emEntrevista ? 2 : 1} />

      <section className="painel-triagem">
        <Disclaimer
          confirmado={avisoConfirmado || emEntrevista}
          aoConfirmar={emEntrevista ? undefined : setAvisoConfirmado}
        />

        {!emEntrevista ? (
          <form onSubmit={enviarSintomas} aria-label="Sintomas" noValidate>
            <h1>Quais seus principais sintomas?</h1>
            <p>Escolha os sintomas que você está sentindo ou descreva com suas palavras. Você pode escolher mais de um.</p>

            <fieldset className="sintomas-comuns">
              <legend>Selecione seus sintomas</legend>
              {SINTOMAS_COMUNS.map((sintoma) => (
                <button
                  key={sintoma}
                  type="button"
                  className="opcao-sintoma"
                  aria-pressed={escolhidos.includes(sintoma)}
                  onClick={() => alternarSintoma(sintoma)}
                >
                  {sintoma}
                </button>
              ))}
            </fieldset>

            <label htmlFor="descricao">Ou descreva com suas palavras</label>
            <textarea
              id="descricao"
              value={descricao}
              maxLength={MAXIMO_CARACTERES}
              rows={4}
              placeholder="Ex.: dor de cabeça forte desde ontem, que piora com a luz"
              aria-describedby="contador-descricao"
              aria-invalid={Boolean(erroSintomas)}
              onChange={(e) => setDescricao(e.target.value)}
            />
            <p id="contador-descricao" className="contador">{descricao.length}/{MAXIMO_CARACTERES}</p>
            {erroSintomas && <p className="field-error">{erroSintomas}</p>}

            <label htmlFor="idade">Idade</label>
            <input
              id="idade"
              type="number"
              inputMode="numeric"
              min={1}
              max={120}
              value={idade}
              aria-invalid={Boolean(erroIdade)}
              onChange={(e) => setIdade(e.target.value)}
              className="campo-idade"
            />
            {erroIdade && <p className="field-error">{erroIdade}</p>}

            {blocoErro}
            {triagem.carregando && <IndicadorCarregamento mensagem="Preparando a primeira pergunta..." demorando={triagem.demorando} />}

            <div className="acoes">
              <button type="submit" className="botao-primario" disabled={!avisoConfirmado || triagem.carregando}>
                Continuar <span aria-hidden="true">→</span>
              </button>
            </div>
          </form>
        ) : (
          <div className="entrevista">
            <h1>Detalhe dos sintomas</h1>
            <p>Responda às perguntas para que a orientação fique mais precisa.</p>

            <ol className="chat" aria-live="polite" aria-label="Conversa da triagem">
              {triagem.mensagens.map((mensagem, indice) => (
                <BalaoChat key={indice} papel={mensagem.papel} conteudo={mensagem.conteudo} />
              ))}
            </ol>

            {triagem.aviso && <p className="aviso-limitacao">{triagem.aviso}</p>}
            {blocoErro}
            {triagem.carregando && (
              <IndicadorCarregamento
                mensagem={triagem.completa ? 'Analisando suas respostas...' : 'Pensando na próxima pergunta...'}
                demorando={triagem.demorando}
              />
            )}

            {!triagem.completa && (
              <form onSubmit={enviarResposta} className="form-resposta" aria-label="Responder">
                <label htmlFor="resposta">Sua resposta</label>
                <div className="linha-resposta">
                  <textarea
                    id="resposta"
                    rows={2}
                    maxLength={MAXIMO_CARACTERES}
                    value={resposta}
                    onChange={(e) => setResposta(e.target.value)}
                  />
                  <button type="submit" className="botao-primario" disabled={triagem.carregando || !resposta.trim()}>Enviar</button>
                </div>
                <button
                  type="button"
                  className="botao-secundario"
                  disabled={triagem.carregando}
                  onClick={() => triagem.encerrar()}
                >
                  Encerrar entrevista e ver resultado
                </button>
              </form>
            )}
          </div>
        )}
      </section>
    </div>
  );
}
