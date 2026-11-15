# Requisitos da Matriz

## Origem e estado

Fonte: Ficha Prática Laboratorial 01, enquadramento do projeto. Facto F-01: o cliente pretende que as equipas organizem projetos, giram tarefas e acompanhem o progresso. A ficha confirma que funcionalidades detalhadas, regras, tecnologias e prioridades ainda não foram definidas. Os requisitos abaixo são propostas para validação, apoiadas nos pressupostos P-01 a P-09 de [constraints.md](constraints.md).

## Visão e objetivos

Disponibilizar um espaço de trabalho partilhado para responder a três perguntas: que trabalho existe, quem é responsável e em que estado se encontra. Critério de utilidade proposto: um membro autorizado consegue criar uma tarefa, atribuí-la a um membro do projeto, alterar o estado e observar a atualização do progresso.

## Utilizadores propostos

| Perfil | Responsabilidades |
| --- | --- |
| Utilizador autenticado | Criar um projeto e consultar os projetos de que é membro |
| Gestor do projeto | Criador do projeto; gere membros e executa as operações de membro |
| Membro do projeto | Consulta e cria tarefas, atribui responsáveis e atualiza estados dentro do projeto |

Os papéis são por projeto: ser gestor de um projeto não concede acesso a outro. Contas iniciais são disponibilizadas por um processo a definir; registo público e administração global não integram este MVP (P-01).

## Requisitos funcionais propostos

Todas as funcionalidades seguintes são Must para o MVP proposto. Esta prioridade é uma proposta do exercício, não uma confirmação do cliente.

| ID | Funcionalidade | Justificação | História | Pressuposto |
| --- | --- | --- | --- | --- |
| RF-01 | Iniciar e terminar uma sessão de utilizador | Identificar quem acede e proteger o trabalho | US-01 | P-01, P-02 |
| RF-02 | Criar projeto e listar apenas projetos acessíveis | Organizar e localizar trabalho | US-02 | P-02, P-03 |
| RF-03 | Adicionar contas existentes como membros | Permitir colaboração controlada | US-03 | P-02 |
| RF-04 | Criar e listar tarefas de um projeto | Registar o trabalho a executar | US-04 | P-03, P-04 |
| RF-05 | Atribuir ou retirar o responsável de uma tarefa | Tornar a responsabilidade explícita | US-05 | P-05 |
| RF-06 | Alterar o estado de uma tarefa | Acompanhar a execução | US-06 | P-04 |
| RF-07 | Apresentar totais e percentagem de conclusão por projeto | Permitir acompanhamento objetivo | US-07 | P-06 |

## Regras de negócio propostas

- Cada projeto tem um gestor e zero ou mais membros adicionais. O gestor é também membro. Apenas ele adiciona membros; um membro não altera permissões.
- Cada tarefa pertence a um único projeto e tem título obrigatório de 1 a 120 caracteres após remover espaços nas extremidades. A descrição é opcional, até 2000 caracteres. Um projeto tem nome de 1 a 100 caracteres após a mesma normalização. Não se exige unicidade dos nomes ou títulos.
- Uma tarefa nasce em “Por fazer”. Estados possíveis: “Por fazer”, “Em curso” e “Concluída”. Qualquer membro pode efetuar qualquer transição entre estes estados, incluindo reabrir uma tarefa.
- O responsável é opcional e, quando existe, é um membro do mesmo projeto. Adicionar novamente um membro não duplica a associação.
- Progresso = tarefas concluídas / total de tarefas × 100, arredondado a uma casa decimal. Sem tarefas: apresentar zero tarefas e “Sem tarefas”, sem efetuar divisão por zero.
- Pedidos inválidos ou não autorizados não alteram dados. Os dados confirmados continuam disponíveis após recarregar a aplicação.

## Requisitos não funcionais propostos

| ID | Requisito e forma de verificação | Base |
| --- | --- | --- |
| RNF-01 | Verificar identidade e acesso ao projeto em todas as operações protegidas, incluindo pedidos diretos; testar utilizadores sem sessão e membros de outro projeto | P-01, P-02 |
| RNF-02 | Não expor credenciais, sessões ou dados de projetos em mensagens públicas ou logs; rever configuração e exemplos de logs antes da entrega | P-07 |
| RNF-03 | Em validação recusada, apresentar o motivo ao utilizador autorizado, preservar os campos preenchidos e não guardar dados parciais; verificar com títulos vazios e demasiado longos | P-03, P-08 |
| RNF-04 | Permitir operar os fluxos principais por teclado, com foco visível e campos identificados; realizar verificação manual antes da entrega | P-08 |
| RNF-05 | Após criação ou alteração confirmada, nova consulta devolve os mesmos dados; verificar também após nova sessão | P-09 |

Metas de latência, disponibilidade, capacidade e recuperação ainda precisam de valores acordados e condições de medição. Não são apresentadas como compromissos aprovados.

## Fora do MVP

| Funcionalidade futura | Prioridade proposta | Motivo de adiamento |
| --- | --- | --- |
| Comentários e anexos | Should posterior | Úteis para colaboração, mas dispensáveis para validar o fluxo central |
| Prazos, prioridades de tarefas e filtros avançados | Should posterior | Dependem de clarificação do processo de trabalho |
| Notificações e integrações externas | Could posterior | Acrescentam dependências e operação |
| Relatórios avançados e histórico de alterações | Could posterior | O MVP oferece apenas progresso atual |
| Registo público, recuperação de conta e administração global | Por avaliar | Processo de identidade ainda pendente |
| Eliminação, arquivo, remoção de membros e transferência de gestor | Por avaliar | Exigem política de retenção e tratamento de tarefas atribuídas |
| Múltiplas organizações, aplicações móveis nativas e automações por IA | Fora do âmbito inicial | Não resultam da necessidade inicial confirmada |

A IA é utilizada no desenvolvimento; não há requisito confirmado para funcionalidades de IA no produto.
