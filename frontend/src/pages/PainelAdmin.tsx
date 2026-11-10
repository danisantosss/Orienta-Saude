import { useEffect, useState } from 'react';
import axios from 'axios';
import { obterIndicadores } from '../api/admin';
import { ROTULOS_URGENCIA } from '../components/BadgeUrgencia';
import Icone from '../components/Icone';
import IndicadorCarregamento from '../components/IndicadorCarregamento';
import type { ErroResponse, IndicadoresResponse, NivelUrgencia, TotalPorPeriodo } from '../types';

const NIVEIS: NivelUrgencia[] = ['LEVE', 'MODERADO', 'URGENTE', 'EMERGENCIA'];
const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

/** "2026-11-09" -> "09/11/2026", sem passar por Date (evita erro de fuso horário). */
function formatarData(data: string): string {
  const [ano, mes, dia] = data.split('-');
  return `${dia}/${mes}/${ano}`;
}

/** Rótulo curto do eixo: "09/11" por dia, "nov/26" por mês. */
function rotuloPeriodo(item: TotalPorPeriodo, agrupamento: IndicadoresResponse['agrupamento']): string {
  const [ano, mes, dia] = item.inicio.split('-');
  return agrupamento === 'MES' ? `${MESES[Number(mes) - 1]}/${ano.slice(2)}` : `${dia}/${mes}`;
}

function porcentagem(parte: number, total: number): number {
  return total === 0 ? 0 : Math.round((parte / total) * 100);
}

function GraficoPorPeriodo({ dados }: { dados: IndicadoresResponse }) {
  const maior = Math.max(1, ...dados.triagensPorPeriodo.map((item) => item.total));
  // Com muitos dias, só alguns rótulos aparecem no eixo para não embolar
  const passoRotulo = Math.max(1, Math.ceil(dados.triagensPorPeriodo.length / 6));
  const unidade = dados.agrupamento === 'MES' ? 'mês' : 'dia';

  return (
    <section className="cartao grafico-cartao" aria-labelledby="titulo-periodo">
      <h2 id="titulo-periodo">Triagens por {unidade}</h2>
      <div className="grafico-barras" aria-hidden="true">
        {dados.triagensPorPeriodo.map((item, indice) => (
          <div key={item.inicio} className="grafico-coluna" title={`${rotuloPeriodo(item, dados.agrupamento)}: ${item.total}`}>
            <span className="grafico-barra" style={{ height: `${(item.total / maior) * 100}%` }} />
            <span className="grafico-rotulo">{indice % passoRotulo === 0 ? rotuloPeriodo(item, dados.agrupamento) : ''}</span>
          </div>
        ))}
      </div>
      {/* A mesma informação em tabela, para leitores de tela */}
      <table className="somente-leitor">
        <caption>Triagens por {unidade}</caption>
        <thead><tr><th scope="col">Período</th><th scope="col">Triagens</th></tr></thead>
        <tbody>
          {dados.triagensPorPeriodo.map((item) => (
            <tr key={item.inicio}><td>{rotuloPeriodo(item, dados.agrupamento)}</td><td>{item.total}</td></tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function GraficoUrgencia({ dados }: { dados: IndicadoresResponse }) {
  const totalResultados = NIVEIS.reduce((soma, nivel) => soma + dados.distribuicaoUrgencia[nivel], 0);

  return (
    <section className="cartao grafico-cartao" aria-labelledby="titulo-urgencia">
      <h2 id="titulo-urgencia">Distribuição por nível de urgência</h2>
      <ul className="grafico-horizontal">
        {NIVEIS.map((nivel) => {
          const quantidade = dados.distribuicaoUrgencia[nivel];
          const fatia = porcentagem(quantidade, totalResultados);
          return (
            <li key={nivel}>
              <span className="grafico-horizontal-rotulo">{ROTULOS_URGENCIA[nivel]}</span>
              <span className="grafico-horizontal-trilho" aria-hidden="true">
                <span className={`grafico-horizontal-barra barra-${nivel.toLowerCase()}`} style={{ width: `${fatia}%` }} />
              </span>
              <span className="grafico-horizontal-valor">{quantidade} ({fatia}%)</span>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

/**
 * Painel administrativo (rota /admin): indicadores agregados e anônimos de uso.
 * Não mostra nenhuma triagem individual, sintoma ou dado de quem usou a plataforma.
 */
export default function PainelAdmin() {
  const [inicio, setInicio] = useState('');
  const [fim, setFim] = useState('');
  const [dados, setDados] = useState<IndicadoresResponse | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    let ativo = true;
    setErro(null);
    setCarregando(true);
    obterIndicadores({ inicio, fim })
      .then((resposta) => { if (ativo) setDados(resposta); })
      .catch((err) => {
        if (!ativo) return;
        const mensagem = axios.isAxiosError<ErroResponse>(err) ? err.response?.data?.mensagem : undefined;
        setErro(mensagem ?? 'Não foi possível carregar os indicadores.');
      })
      .finally(() => { if (ativo) setCarregando(false); });
    return () => { ativo = false; };
  }, [inicio, fim]);

  return (
    <div className="pagina-admin">
      <header className="cabecalho-admin">
        <h1>Painel administrativo</h1>
        <p className="texto-secundario">
          Indicadores de uso da plataforma. Só números agregados: nenhum sintoma ou dado pessoal aparece aqui.
        </p>
      </header>

      <form className="cartao filtros-admin" aria-label="Período" onSubmit={(e) => e.preventDefault()}>
        <div className="campo">
          <label htmlFor="admin-inicio">De</label>
          <input id="admin-inicio" type="date" value={inicio} max={fim || undefined}
            onChange={(e) => setInicio(e.target.value)} />
        </div>
        <div className="campo">
          <label htmlFor="admin-fim">Até</label>
          <input id="admin-fim" type="date" value={fim} min={inicio || undefined}
            onChange={(e) => setFim(e.target.value)} />
        </div>
        <button type="button" className="botao-secundario" disabled={!inicio && !fim}
          onClick={() => { setInicio(''); setFim(''); }}>
          Últimos 30 dias
        </button>
      </form>

      {erro && <p role="alert" className="form-error">{erro}</p>}
      {carregando && <IndicadorCarregamento mensagem="Carregando os indicadores..." />}

      {!erro && dados && (
        <div className={carregando ? 'atualizando' : ''}>
          <p className="periodo-admin">
            Período: {formatarData(dados.inicio)} a {formatarData(dados.fim)}
          </p>

          <div className="numeros-admin">
            <div className="cartao numero-admin">
              <Icone nome="prancheta" />
              <strong>{dados.totalTriagens}</strong>
              <span>triagens iniciadas</span>
            </div>
            <div className="cartao numero-admin">
              <Icone nome="grafico" />
              <strong>{dados.triagensFinalizadas}</strong>
              <span>entrevistas finalizadas</span>
            </div>
            <div className="cartao numero-admin">
              <Icone nome="alerta" />
              <strong>{dados.taxaEncerramentoAntecipado.toLocaleString('pt-BR')}%</strong>
              <span>encerradas antes da conclusão ({dados.encerradasAntecipadamente})</span>
            </div>
          </div>

          {dados.totalTriagens === 0 ? (
            <div className="cartao"><p>Nenhuma triagem neste período.</p></div>
          ) : (
            <div className="graficos-admin">
              <GraficoPorPeriodo dados={dados} />
              <GraficoUrgencia dados={dados} />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
