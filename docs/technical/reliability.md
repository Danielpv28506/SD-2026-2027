# Fiabilidade e limitações

## Garantias implementadas

UDP pode perder, duplicar e reordenar datagramas. Este projeto compensa apenas
algumas dessas propriedades:

- `lastDelivered` regista a fronteira contígua já entregue;
- mensagens futuras ficam em `mensagensTemporarias` até a lacuna fechar;
- ao fechar a lacuna, o servidor entrega e remove todas as mensagens consecutivas;
- IDs menores ou iguais a `lastDelivered` nunca voltam a ser entregues;
- `putIfAbsent` conserva a primeira versão recebida de um ID futuro duplicado;
- o cliente espera no máximo 3000 ms por cada resposta;
- ao iniciar ou reentrar no modo automático, `GET_NEXT_ID` ressincroniza o
  cliente com o estado do servidor.

O cliente não incrementa o ID num timeout. Assim, a próxima tentativa reutiliza
o mesmo ID. Se o primeiro datagrama tiver sido entregue e apenas a resposta tiver
sido perdida, o servidor identifica o reenvio como duplicado e responde
`waitingfor` com o próximo ID; o cliente pode então avançar em segurança. O mesmo
valor também faz o contador recuar se a resposta revelar um restart do servidor.

## Reinícios

Reiniciar só o cliente não afeta `lastDelivered`: uma nova sincronização recupera
o próximo ID. Reiniciar o servidor apaga `lastDelivered`, a lista entregue e o
buffer, pois não existe persistência. A próxima sincronização devolve 1 e o
cliente deve adotar esse valor.

## Limitações restantes

- não há retransmissão automática: após timeout, o utilizador volta a submeter o
  payload;
- uma mensagem futura perdida deixa as posteriores no buffer indefinidamente;
- o buffer não tem limite nem expiração;
- a sequência é global, pelo que vários clientes automáticos concorrentes podem
  obter o mesmo próximo ID; não existe reserva atómica de IDs;
- não há autenticação, cifragem, checksum da aplicação ou proteção contra um
  emissor malicioso;
- o estado desaparece num restart do servidor.

Portanto, a aplicação não transforma UDP em TCP e não oferece fiabilidade
absoluta. Implementa ordenação, supressão de duplicados já entregues,
sincronização e falha rápida adequadas ao exercício.
