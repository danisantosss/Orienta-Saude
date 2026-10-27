import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listarHistorico } from '../api/triagem';
import BadgeUrgencia, { ROTULOS_URGENCIA } from '../components/BadgeUrgencia';
import Icone from '../components/Icone';
import IndicadorCarregamento from '../components/IndicadorCarregamento';
import type { FiltroHistorico, NivelUrgencia, Pagina, SessaoTriagemResponse } from '../types';

const LIMITE_SINTOMAS = 120;

const NIVEIS: NivelUrgencia[] = ['LEVE', 'MODERADO', 'URGENTE', 'EMERGENCIA'];

interface Filtros {
  inicio: string;
  fim: string;
  nivelUrgencia: NivelUrgencia | '';
}

const SEM_FILTROS: Filtros = { inicio: '', fim: '', nivelUrgencia: '' };

function formatarData(data: string): string {
  const quando = new Date(data);
  const dia = quando.toLocaleDateString('pt-BR');
  const hora = quando.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  return `${dia} às ${hora}`;
}

function resumirSintomas(sintomas: string): string {
  return sintomas.length > LIMITE_SINTOMAS ? `${sintomas.slice(0, LIMITE_SINTOMAS)}…` : sintomas;
}

/** Só manda para a API os filtros preenchidos. */
function montarConsulta(pagina: number, filtros: Filtros): FiltroHistorico {
  const consulta: FiltroHistorico = { pagina };
  if (filtros.nivelUrgencia) consulta.nivelUrgencia = filtros.nivelUrgencia;
  if (filtros.inicio) consulta.inicio = filtros.inicio;
  if (filtros.fim) consulta.fim = filtros.fim;
  return consulta;
}

/**
 * Histórico de triagens (rota /historico): lista paginada com filtros por período e
 * nível de urgência. Cada triagem concluída abre o resultado completo.
 */
export default function Historico() {
  const [filtros, setFiltros] = useState<Filtros>(SEM_FILTROS);
  const [pagina, setPagina] = useState(0);
  const [dados, setDados] = useState<Pagina<SessaoTriagemResponse> | null>(null);
  const [erro, setErro] = useState(false);
  const [carregando, setCarregando] = useState(true);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    let ativo = true;
    setErro(false);
    setCarregando(true);
    listarHistorico(montarConsulta(pagina, filtros))
      .then((resposta) => { if (ativo) setDados(resposta); })
      .catch(() => { if (ativo) setErro(true); })
      .finally(() => { if (ativo) setCarregando(false); });
    return () => { ativo = false; };
  }, [pagina, filtros, tentativa]);

  const alterarFiltro = useCallback((campo: keyof Filtros, valor: string) => {
    setFiltros((atuais) => ({ ...atuais, [campo]: valor }));
    setPagina(0);
  }, []);

  function limparFiltros() {
    setFiltros(SEM_FILTROS);
    setPagina(0);
  }

  const temFiltros = Object.values(filtros).some(Boolean);

  return (
    <div className="pagina-historico">
      <header className="cabecalho-historico">
        <h1>Histórico de triagens</h1>
        <p className="texto-secundario">Acompanhe suas triagens anteriores e os resultados obtidos.</p>
      </header>

      <div className="grade-historico">
        <aside className="cartao filtros-historico" aria-labelledby="titulo-filtros">
          <h2 id="titulo-filtros">Filtros</h2>
          <fieldset>
            <legend>Período</legend>
            <label htmlFor="filtro-inicio">De</label>
            <input id="filtro-inicio" type="date" value={filtros.inicio} max={filtros.fim || undefined}
              onChange={(e) => alterarFiltro('inicio', e.target.value)} />
            <label htmlFor="filtro-fim">Até</label>
            <input id="filtro-fim" type="date" value={filtros.fim} min={filtros.inicio || undefined}
              onChange={(e) => alterarFiltro('fim', e.target.value)} />
          </fieldset>
          <label htmlFor="filtro-nivel">Nível de urgência</label>
          <select id="filtro-nivel" value={filtros.nivelUrgencia}
            onChange={(e) => alterarFiltro('nivelUrgencia', e.target.value)}>
            <option value="">Todos os níveis</option>
            {NIVEIS.map((nivel) => <option key={nivel} value={nivel}>{ROTULOS_URGENCIA[nivel]}</option>)}
          </select>
          <button type="button" className="botao-secundario" onClick={limparFiltros} disabled={!temFiltros}>
            <Icone nome="lixeira" tamanho={18} /> Limpar filtros
          </button>
        </aside>

        <section className={`lista-historico ${carregando && dados ? 'atualizando' : ''}`}
          aria-label="Triagens anteriores" aria-busy={carregando}>
          {erro && (
            <div className="cartao">
              <p role="alert" className="form-error">Não foi possível carregar o histórico.</p>
              <button type="button" className="botao-primario" onClick={() => setTentativa((n) => n + 1)}>
                Tentar novamente
              </button>
            </div>
          )}

          {!erro && !dados && <IndicadorCarregamento mensagem="Carregando o histórico..." />}
          {/* Ao trocar filtro ou página, a lista antiga fica apagada até chegar a nova, para não parecer o resultado do filtro novo */}
          {!erro && dados && carregando && <IndicadorCarregamento mensagem="Atualizando o histórico..." />}

          {!erro && dados && dados.itens.length === 0 && (
            <div className="cartao historico-vazio">
              {temFiltros ? (
                <p>Nenhuma triagem encontrada com esses filtros.</p>
              ) : (
                <>
                  <p>Você ainda não fez nenhuma triagem.</p>
                  <Link to="/triagem" className="botao-primario">Fazer minha primeira triagem</Link>
                </>
              )}
            </div>
          )}

          {!erro && dados && dados.itens.length > 0 && (
            <>
              <ul>
                {dados.itens.map((item) => (
                  <li key={item.id} className={`cartao item-historico item-${item.nivelUrgencia?.toLowerCase() ?? 'pendente'}`}>
                    <span className="item-historico-icone"><Icone nome="virus" tamanho={32} /></span>
                    <div className="item-historico-texto">
                      <time dateTime={item.iniciadaEm}>{formatarData(item.iniciadaEm)}</time>
                      <p className="item-historico-sintomas">{resumirSintomas(item.sintomasIniciais)}</p>
                    </div>
                    {item.nivelUrgencia ? (
                      <BadgeUrgencia nivel={item.nivelUrgencia} />
                    ) : (
                      <span className="texto-secundario">Não concluída</span>
                    )}
                    {item.status === 'CONCLUIDA' && (
                      <Link to={`/resultado/${item.id}`} className="botao-secundario">
                        Ver detalhes<span className="somente-leitor"> da triagem de {formatarData(item.iniciadaEm)}</span>
                      </Link>
                    )}
                  </li>
                ))}
              </ul>

              {dados.totalPaginas > 1 && (
                <nav className="paginacao" aria-label="Paginação do histórico">
                  <button type="button" className="botao-secundario" aria-label="Página anterior"
                    disabled={dados.pagina === 0} onClick={() => setPagina(dados.pagina - 1)}>
                    ← Anterior
                  </button>
                  <span>Página {dados.pagina + 1} de {dados.totalPaginas}</span>
                  <button type="button" className="botao-secundario" aria-label="Próxima página"
                    disabled={dados.pagina + 1 >= dados.totalPaginas} onClick={() => setPagina(dados.pagina + 1)}>
                    Próxima →
                  </button>
                </nav>
              )}
            </>
          )}
        </section>
      </div>
    </div>
  );
}
