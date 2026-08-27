# Changelog geral — Pacote RP Custom

Este documento descreve o que foi efetivamente adicionado ao build atual do mod para Minecraft 1.12.2. Os itens marcados com `[x]` estão implementados. Os itens marcados com `[ ]` continuam como planejamento e não devem ser anunciados como recurso jogável.

## Sistema de atributos e progressão

- [x] Adicionado menu de Status Shinobi com seis atributos: Velocidade, Força, Resistência, Vida, Chakra Máximo e SPI.
- [x] Adicionados botões de distribuição `+1`, `+10` e `MAX`.
- [x] A interface mostra apenas `pontos disponíveis / pontos totais`, removendo a capacidade paralela que tornava a distribuição confusa.
- [x] Números a partir de um milhão são abreviados na GUI (`1.00M`, `100.00M`) para não cortar ou sobrepor valores.
- [x] Corrigidos estouros e inconsistências ao configurar quantidades altas de pontos.
- [x] Pontos e limites são armazenados como valores longos, preservados ao morrer e sincronizados entre servidor e cliente.
- [x] Dados antigos são migrados automaticamente para o formato atual.
- [x] O limite técnico de cada atributo passou a 100.000.000 para permitir progressão longa no estilo DBC.
- [x] Adicionado teto pessoal de atributo: ele substitui o padrão do rank somente para o jogador escolhido pela staff.
- [x] Existe uma única reserva total de pontos. Pontos disponíveis são sempre calculados como `total - atributos distribuídos`.
- [x] O teto por atributo continua separado da reserva total: o jogador não pode passar do teto, mas decide livremente como distribuir os pontos recebidos.

### Limite padrão por rank

- [x] Os ranks definem apenas o limite máximo de cada atributo; a staff concede a reserva total de pontos separadamente.
- [x] Promover ou rebaixar não altera Ninja XP, Battle XP nem concede pontos escondidos.
- [x] O total limitado de pontos impede o jogador de maximizar todos os atributos e mantém a escolha de build importante.
- [x] A staff pode alterar o limite individual de cada rank para todo o mundo.
- [x] Jogadores do mesmo rank podem possuir tetos pessoais diferentes, permitindo prodígios, veteranos e recompensas narrativas sem criar ranks artificiais.

### Efeito de cada atributo

- [x] Velocidade aumenta o movimento e participa de uma disputa de esquiva contra a Velocidade do atacante.
- [x] A esquiva por Velocidade só aparece quando o defensor possui vantagem e cresce competitivamente até aproximadamente 20% em diferenças extremas.
- [x] Força usa crescimento por potência e aumenta continuamente o dano básico e o dano das técnicas de Taijutsu.
- [x] Força acima do limite visual de ataque do Minecraft é preservada como multiplicador de dano corpo a corpo.
- [x] Como referência, 100.000 de Força fornecem aproximadamente +400 de ataque efetivo; 1.000.000 fornecem aproximadamente +2.524.
- [x] Resistência gera uma classificação de defesa contínua. Não existe mais o teto fixo de 28%.
- [x] Vida gera HP efetivo contínuo. Quando a barra chega ao limite seguro do Minecraft, o dano é comprimido para preservar a durabilidade adicional.
- [x] Chakra Máximo usa crescimento por potência, aumenta a reserva e continua acelerando a canalização através do nível de Chakra.
- [x] Os quatro atributos físicos também aumentam a reserva de stamina usada por Taijutsu.
- [x] SPI combina regeneração fixa progressiva com uma porcentagem crescente da reserva máxima e reduz a trava após receber dano.
- [x] A regeneração pode começar após cerca de 2 segundos parado; a trava de combate cai gradualmente de 5 para até 1 segundo em valores extremos de SPI.
- [x] Corrigido o reset indevido do contador de repouso causado pelo próprio tick de regeneração; a taxa exibida por segundo agora é realmente aplicada continuamente.
- [x] Os atributos usam crescimento sublinear por potência: números maiores são controlados, mas nunca convergem para um bônus fixo.
- [x] Afinidade elemental concede 35% a mais de XP ao treinar jutsus daquela natureza.

## Balanceamento dos jutsus customizados
T

### Jutsus customizados incluídos

- [x] Ninjutsu: Crow Clone, Crow Trap Clone, Explosive Clone, Shuriken Shadow Clone, Fire Rasengan, Sensorial Jutsu e Chakra Pulse.
- [x] Katon: Fire Phoenix, Housenka e Housenka Tsumabeni.
- [x] Suiton: Water Clone, Mizuame Nabara, Water Wall e Water Prison Trap.
- [x] Raiton: Lightning Clone e Chidori Senbon.
- [x] Doton: Retsudo Tensho.
- [x] Inton/Genjutsu: False Opening, Memory Fracture, Murder Intent, Illusionary Execution e Burning Coffin.
- [x] Taijutsu: Leaf Whirlwind, Leaf Hurricane, Dynamic Entry, Primary Lotus e Lion Combo.
- [x] Inuzuka: Ninken Companion/Akamaru.
- [x] Cada técnica possui pergaminho com indicação de rank para distribuição por treino, missão ou staff.

## Apresentação visual dos jutsus

- [x] Os jutsus customizados, exceto Raiton, receberam apresentação própria em vez de compartilhar apenas partículas genéricas.
- [x] Foram adicionados arcos de ataque, ondas, círculos, selos, fumaça, fogo, água, impactos, sons e tremor de câmera sincronizado.
- [x] Katon possui cones de fogo, arcos quentes e impactos mais pesados.
- [x] Suiton possui água, bolhas, selos e paredes com impacto visual.
- [x] Clones possuem aparições distintas: corvos, fumaça, selo de armadilha e preparação explosiva.
- [x] Fire Rasengan recebeu esfera flamejante e impacto próprio.
- [x] Sensorial Jutsu recebeu ondas concêntricas de detecção.
- [x] Retsudo Tensho recebeu ruptura de terra, poeira e tremor.
- [x] Chakra Pulse recebeu explosão radial de chakra.
- [x] Raiton foi deliberadamente excluído desta rodada visual, conforme solicitado.

## Taijutsu e stamina

- [x] Taijutsu consome stamina em vez de Chakra.
- [x] O acesso reconhece clãs de Taijutsu/Lee e também jogadores que abriram os Oito Portões.
- [x] O dano escala com Força e Velocidade, dando significado real a builds físicas.
- [x] Leaf Whirlwind funciona como golpe inicial de alvo único; custo base 35 e cooldown efetivo mínimo de 4 s.
- [x] Leaf Hurricane atinge o alvo e uma área curta; custo base 55 e cooldown mínimo de 7 s.
- [x] Dynamic Entry executa avanço de longa distância; custo base 65 e cooldown mínimo de 7 s.
- [x] Primary Lotus lança, causa dano elevado e aplica lentidão; custo base 95 e cooldown mínimo de 12 s.
- [x] Lion Combo executa sequência, aplica fraqueza e empurrão; custo base 85 e cooldown mínimo de 12 s.
- [x] Cada golpe recebeu trilhas, impactos e intensidade de câmera diferentes.

## Genjutsu e experiência da vítima

- [x] A vítima recebe efeitos visuais e de câmera somente no próprio cliente; sua posição real não é transferida para outra dimensão.
- [x] Para os outros jogadores, a vítima permanece no local onde foi capturada, preservando o combate e o RP ao redor.
- [x] False Opening distorce a tela e inverte os controles de movimento.
- [x] Memory Fracture usa cortes, pulsos roxos, fadiga e desorientação.
- [x] Murder Intent reduz o movimento, aplica fraqueza e cria uma vinheta vermelha ameaçadora.
- [x] Illusionary Execution bloqueia o movimento e renderiza um cenário vermelho com névoa, flashes invertidos, cruz, braços presos, o modelo/skin da própria vítima e lâminas repetidas com sons de dano.
- [x] Burning Coffin aplica calor, fogo, fumaça, vinheta e tremor de câmera.
- [x] A duração dos genjutsus cresce com a maestria.
- [x] Chakra Pulse permite quebrar um genjutsu quando o Battle XP do defensor é suficiente para superar o poder registrado do conjurador.
- [x] Uma quebra bem-sucedida remove paralisia, náusea, cegueira, fraqueza, lentidão, fadiga e fogo.

## Sharingan e dōjutsu

- [x] Removida a esquiva automática de 60% do Sharingan.
- [x] O Sharingan ainda mantém percepção visual e rastreamento de alvo, mas não cancela ataques gratuitamente.
- [x] A esquiva pequena do atributo Velocidade continua existindo e compara defensor e atacante.
- [x] Adicionada progressão separada de 1, 2 e 3 tomoe.
- [x] Cada estágio custa 1 ponto de RP e exige clã Uchiha no menu de progressão.
- [x] Adicionadas teclas para ativar/trocar e confirmar o dōjutsu, com cooldown de 20 segundos entre ativações.
- [x] Adicionado comando local `/eyes`.
- [x] O editor permite ajustar somente a altura do olho entre -2 e +2, que é o ajuste necessário para alinhar o Sharingan à skin.
- [x] A camada dos olhos agora é filha da transformação da cabeça: altura e profundidade acompanham corretamente yaw/pitch em vez de deslocarem no espaço global.
- [x] O afastamento frontal foi reduzido de 0,12 para 0,025 unidade de modelo, suficiente para evitar z-fighting sem deixar os olhos flutuando diante do rosto.
- [x] A camada fullbright da íris usa a mesma transformação local, evitando separação ou desaparecimento quando a cabeça é vista em ângulo.
- [x] A alteração possui prévia ao vivo, Salvar, Resetar e Cancelar.
- [x] A posição é salva no jogador, sincronizada para observadores e preservada entre login, morte e respawn.
- [x] Texturas de 1, 2 e 3 tomoe refeitas em atlas 2048×512.
- [x] Íris aumentadas para 52×52 na área útil, com vermelho bordô inspirado no anime, sombreamento limpo e brilho/fullbright preservado.
- [x] Dois tomoe posicionados na diagonal; três tomoe posicionados em 12, 4 e 8 horas com caudas visíveis.

### Sharingan Copy

- [x] Exige Sharingan de 3 tomoe vinculado ao proprietário.
- [x] O usuário precisa observar outro jogador conjurando ou ter observado a técnica nos últimos 5 segundos.
- [x] Alcance de leitura de até 32 blocos.
- [x] Custo de tentativa: o maior valor entre 150 de Chakra e 35% do Chakra atual.
- [x] Cooldown de tentativa: 400 segundos.
- [x] Chance de sucesso começa em 70% e chega a 90% com maestria.
- [x] Progressão de rank copiável: D inicialmente, C com 25%, B com 50% e A com 75% de maestria.
- [x] Técnicas rank S não podem ser copiadas.
- [x] Somente Ninjutsu, Doton, Futon, Katon, Raiton, Suiton e Iryō comuns são aceitos.
- [x] Kekkei Genkai, transformações, bijū, Senjutsu, Mangekyō, Amaterasu, Kamui, Susanoo, Limbo, marionetes, insetos e outras técnicas exclusivas são bloqueadas.
- [x] A cópia guarda 60% do XP observado, pertence somente ao copiador e desaparece após 60 segundos.
- [x] O jogador mantém apenas uma cópia temporária por vez.
- [x] O uso da técnica copiada preserva custo relevante, com mínimo de 70% do custo base.

## Treinamento elemental

- [x] Adicionados pergaminhos de treino para Katon, Suiton, Futon, Doton e Raiton.
- [x] O treino abre um minigame de sequências de selos.
- [x] São cinco sequências e a sessão possui limite máximo de 90 segundos.
- [x] Errar, cancelar ou quebrar a postura encerra o treino sem XP.
- [x] Sucesso concede XP elemental e XP de selos; execução perfeita concede bônus de 20%.
- [x] A recompensa base é 25 XP e passa a 50 XP quando a maestria já alcançou 75%.
- [x] Maestria elemental vai até 2.500 XP/100%.
- [x] Completar um treino adiciona a afinidade correspondente.
- [x] Jutsus elementais ficam bloqueados até o jogador possuir a afinidade ou iniciar progressão naquela natureza.

## Inuzuka e Akamaru

- [x] Adicionada técnica exclusiva do clã Inuzuka para invocar um Ninken companheiro.
- [x] O modelo recria Akamaru com mais camadas de pelo, silhueta menos cúbica e textura 512×512.
- [x] Existe somente um Ninken ativo por proprietário.
- [x] Invocar novamente cura e atualiza o companheiro em vez de duplicá-lo.
- [x] Custo base 80 e cooldown próprio de 15 segundos, além do escalonamento customizado.
- [x] A maestria escala de 50 a 140 HP, 4 a 14 de dano e 0,36 a 0,48 de velocidade.
- [x] O Ninken segue o dono, volta por teleporte quando fica muito distante e ataca o alvo/revidador do proprietário.
- [x] Mordidas aplicam Fraqueza e podem criar um empurrão de combo quando o dono está próximo.

## Invocações de sapo

- [x] A invocação agora possui níveis definidos pela carga do jutsu.
- [x] Carga abaixo de 2,5: Sapo Batedor.
- [x] Carga entre 2,5 e 5,9: Mini Gamakichi.
- [x] Carga a partir de 6: sapo genérico maior.
- [x] Carga máxima a partir de 16: Gamabunta.
- [x] Sapo Batedor: 20 HP, 3 de dano, velocidade 0,28 e detecção através de paredes em 8 blocos.
- [x] Mini Gamakichi: 40 HP, 6 de dano, velocidade 0,28 e detecção em 12 blocos.
- [x] Ambos seguem o invocador, defendem o grupo e retornam por teleporte se ficarem longe demais.
- [x] Modelos e texturas 512×512 seguem o estilo visual de Gamabunta.
- [x] Mini Gamakichi possui pele laranja, manchas, colete azul e tantō nas costas.

## Missões, Bingo Book e ferramentas de RP

- [x] Adicionados Quadro de Missões e Bingo Book com interfaces próprias.
- [x] Não existem missões automáticas genéricas no pool atual: a staff publica o conteúdo de acordo com a história do servidor.
- [x] Missões podem registrar nome, descrição, rank, reputação, rank mínimo, tempo, alvo, área, jogador/equipe designada, recompensa customizada e alerta da vila.
- [x] Suporte a objetivos de viagem, eliminação de hostis, caçada e conclusão narrativa exclusiva da staff.
- [x] Jogadores podem aceitar, acompanhar, entregar ou abandonar uma missão.
- [x] Missões possuem prazo, progresso, proteção contra entrega duplicada e recompensa de Ryō/reputação.
- [x] Missões puramente narrativas só podem ser concluídas pelo Kage/conselho.
- [x] Bingo Book registra crime, nível de ameaça, recompensa, última localização, vila e observações.
- [x] Caçadas precisam ser autorizadas; a morte do alvo correto valida a prova e paga a recompensa configurada, com piso de 1.000 Ryō.
- [x] Painel administrativo mantém arquivos de pistas, eventos, notas de RP e arcos.
- [x] Staff pode promover jogadores e declarar Nukenin diretamente pelo painel.
- [x] Comandos `/rpadmin` e `/adminmissions` abrem o painel para operador ou jogador com rank Hokage.
- [x] Adicionados Documento Shinobi e Passaporte de Vila com emissor, vila e validade opcional.
- [x] `/rpdocument <id|passport> <jogador> <vila> [dias]` pode ser usado por operador ou Hokage.

## Comandos de atributos para staff

- [x] `/statlimit rank <rank> <valor>` define o teto de cada atributo para todo o rank.
- [x] `/statlimit player <jogador> <valor|reset>` define ou remove um teto pessoal usando o mesmo comando.
- [x] `/statpoints <jogador> <valor>` adiciona pontos à reserva total para o jogador distribuir livremente.
- [x] `/setstat <jogador> <atributo> <valor>` define diretamente Speed, Strength, Resistance, Health, Chakra ou SPI.
- [x] `/rpstats checksheet <jogador>` mostra ficha completa.
- [x] `/rpstats statcap <jogador> <valor>` define o teto pessoal de cada atributo; valor 0 volta ao padrão do rank.
- [x] `/rpstats clan <jogador> <clã>` define o clã.
- [x] `/rpstats rank <jogador> <rank>` promove/rebaixa sem alterar a reserva de pontos ou o Battle XP.
- [x] `/rpstats ranklimit <rank> <valor>` altera o limite individual daquele rank no mundo.
- [x] `/rpstats affinity <jogador> <afinidade>` define afinidade.
- [x] `/addaffinity` e `/removeaffinity` permitem múltiplas afinidades.
- [x] `/rpstats stat <jogador> <atributo> <valor>` corrige um atributo específico.
- [x] `/rpstats sharingan <jogador> <0-3>` controla a progressão de tomoe.
- [x] `/rpstats clans`, `/rpstats ranks` e `/rpstats affinities` mostram valores válidos.
- [x] `/setstatcap <jogador> <valor>` agora controla corretamente o teto pessoal de atributo.
- [x] Os aliases antigos principais (`/checksheet`, `/setstatpoints`, `/setstatcap`, `/setclan`, `/setrank`, `/setranklimit`, `/setaffinity`, `/setrpstat` e `/setsharingan`) continuam registrados para compatibilidade.
- [x] `/addninjaxp <jogador> <valor>` continua disponível para progressão administrada.

## Música ambiente controlada pelo servidor

- [x] Adicionado sistema próprio de música ambiente sem exigir um mod de música separado.
- [x] O servidor lê arquivos `.ogg` em `config/narutomod_server_music`.
- [x] Suporta até 512 faixas de até 128 MB cada.
- [x] Ao conectar, o cliente recebe o catálogo e baixa somente arquivos ausentes em blocos controlados.
- [x] Arquivos são validados por tamanho e SHA-256 antes de entrar no cache local.
- [x] O servidor pode tocar para todos ou para um jogador, definir fade, volume e loop.
- [x] Quem entra durante uma música global recebe o estado atual.
- [x] Staff: `/rpmusic list`, `rescan`, `sync`, `play`, `stop` e `now`.
- [x] Jogador: `/music volume 0-100`, `mute`, `unmute`, `stop`, `now` e `cache`.
- [x] O sistema respeita o volume de Música do Minecraft e guarda preferências locais.
- [x] Nenhuma música protegida foi incluída no JAR; a equipe deve distribuir apenas áudio que tenha autorização para usar.

## Conteúdo visual adicional

- [x] Adicionadas abas criativas separadas para Olhos Customizados e Jutsus Customizados.
- [x] Texturas novas do Akamaru, Sapo Batedor, Mini Gamakichi e Sharingan foram empacotadas no build atual.
- [x] O menu de atributos foi redesenhado como um Registro Shinobi aberto, inspirado em ficha oficial de vila e Bingo Book.
- [x] A página esquerda mostra retrato 3D, selo de identificação, rank, clã, afinidade, pontos disponíveis/totais, teto e número de arquivo.
- [x] A página direita organiza os seis atributos em tabela, com ícones Minecraft, valores, barras proporcionais e botões temáticos.
- [x] Os detalhes de efeito aparecem ao passar o mouse sobre cada atributo, reduzindo o excesso de texto permanente.
- [x] A progressão Uchiha ganhou uma página própria dentro do mesmo dossier, sem remover o sistema existente.

## Estado do port e sistemas ainda não implementados

- [x] Existe uma estrutura/rascunho do port para Minecraft 1.20.1, com notas, configuração inicial e ativos copiados para análise.
- [ ] O port 1.20.1 ainda não é uma conversão funcional completa; o build jogável atual continua sendo Minecraft 1.12.2.
- [ ] O sistema formal de mentor/Jōnin, com vagas, avaliações e limites de alunos, ainda está no planejamento.
- [ ] O organizador dedicado de grandes eventos, como Floresta da Morte/Exame Chūnin, ainda não foi implementado como sistema separado.
- [ ] As músicas do anime não fazem parte do mod; somente a infraestrutura de reprodução está implementada.
- [ ] Não foi criada uma dimensão de Genjutsu, pois a cena somente no cliente atingiu o efeito desejado sem mover a vítima no servidor.

## Verificação do build

- [x] Build completo do Forge/Gradle concluído com sucesso.
- [x] JAR atual: `build/libs/modid-1.0.jar`.
- [x] SHA-256 do build revisado: `A97C64AFD7C69D7E397DD2310AB8C636F79CC9F2ABE8B9DC61513460DBFE85F1`.
