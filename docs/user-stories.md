# Backlog inicial

Estado: proposta dependente dos pressupostos em [constraints.md](constraints.md). Todas as histórias têm prioridade Must no MVP proposto; os critérios são especificações para testes futuros, não testes já executados.

## US-01 — Acesso (RF-01)

Como utilizador, quero iniciar e terminar a minha sessão, para aceder ao trabalho com a minha identidade.

- Given uma conta previamente disponibilizada, When forneço credenciais válidas, Then obtenho acesso autenticado aos meus projetos.
- Given credenciais inválidas, When tento entrar, Then o acesso é recusado com mensagem genérica que não confirma a existência da conta.
- Given uma sessão ativa, When termino a sessão, Then essa sessão deixa de autorizar novas operações protegidas.
- Given ausência de sessão válida, When solicito dados de um projeto, Then não recebo dados e nenhuma alteração é efetuada.

## US-02 — Projetos (RF-02)

Como utilizador autenticado, quero criar e consultar os meus projetos, para organizar trabalho separado.

- Given uma sessão válida, When crio um projeto com nome de 1 a 100 caracteres após remover espaços nas extremidades, Then o projeto é guardado e fico como gestor e membro.
- Given projetos A e B, sendo membro apenas de A, When consulto a lista, Then vejo A e não vejo B.
- Given um nome vazio, apenas espaços ou com 101 caracteres após normalização, When submeto, Then recebo erro de validação e não é criado projeto.
- Given nenhum projeto acessível, When consulto a lista, Then vejo um estado vazio explícito.

## US-03 — Membros (RF-03)

Como gestor do projeto, quero adicionar um utilizador existente como membro, para permitir colaboração.

- Given que sou gestor e a conta indicada existe, When a adiciono, Then essa conta passa a consultar e gerir tarefas desse projeto.
- Given uma conta já membro, When volto a adicioná-la, Then continua a existir apenas uma associação e sou informado de que já pertence ao projeto.
- Given que sou membro sem o papel de gestor, When tento adicionar alguém, Then o pedido é recusado e os membros não mudam.
- Given uma conta inexistente, When tento adicioná-la, Then a operação falha sem criar conta ou associação. A forma segura de localizar contas está pendente em Q-02.

## US-04 — Criação de tarefas (RF-04)

Como membro, quero criar uma tarefa num projeto, para registar trabalho a realizar.

- Given acesso ao projeto e título válido de 1 a 120 caracteres, When submeto a tarefa com descrição opcional até 2000 caracteres, Then ela aparece nesse projeto em “Por fazer”, sem responsável.
- Given uma criação confirmada, When recarrego a aplicação, Then a tarefa permanece na listagem com os mesmos dados.
- Given título vazio, apenas espaços, 121 caracteres ou descrição com 2001 caracteres, When submeto, Then não é criada tarefa e os campos são preservados com indicação do erro.
- Given um projeto inexistente ou inacessível, When tento criar uma tarefa, Then a operação é recusada sem criar dados e sem revelar dados de outro projeto.

## US-05 — Responsável (RF-05)

Como membro, quero atribuir um responsável à tarefa, para tornar claro quem acompanha a sua execução.

- Given uma tarefa do meu projeto e um utilizador membro, When o atribuo, Then a tarefa passa a apresentar esse responsável.
- Given um responsável atribuído, When retiro a atribuição, Then a tarefa fica “Sem responsável”.
- Given um utilizador que não pertence ao projeto, When tento atribuí-lo, Then a operação é recusada e a atribuição anterior mantém-se.

## US-06 — Estado (RF-06)

Como membro, quero atualizar o estado de uma tarefa, para comunicar o avanço do trabalho.

- Given uma tarefa acessível, When escolho um dos três estados definidos, Then o novo estado é guardado e apresentado após nova consulta.
- Given uma tarefa concluída, When a altero para “Em curso”, Then é reaberta e deixa de contar como concluída.
- Given um valor fora dos estados permitidos, When o envio, Then o pedido é rejeitado sem alterar a tarefa.
- Given uma tarefa de outro projeto ao qual não pertenço, When tento alterar o estado diretamente, Then o acesso é recusado e a tarefa mantém o estado anterior.

## US-07 — Progresso (RF-07)

Como membro, quero consultar o progresso do projeto, para perceber o trabalho pendente e concluído.

- Given quatro tarefas, duas concluídas, uma em curso e uma por fazer, When consulto o progresso, Then vejo total 4, contagens 2/1/1 e 50,0% de conclusão.
- Given três tarefas e uma concluída, When consulto, Then vejo 33,3%.
- Given um projeto sem tarefas, When consulto, Then vejo total 0 e “Sem tarefas”.
- Given uma alteração de estado confirmada, When atualizo a vista de progresso, Then as contagens refletem o novo estado. Atualização em tempo real não é exigida.

## Cobertura de exceções

Os critérios abrangem sessão inválida, acesso entre projetos, entradas em branco, limites de comprimento, conta inexistente, membro duplicado, responsável externo, estado desconhecido, reabertura e divisão por zero. A política para edições simultâneas e repetição de pedidos após falha de rede está pendente em Q-05 e deve ser definida antes da implementação desses comportamentos.
