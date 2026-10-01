# Protocolo UDP

Todas as mensagens são texto UTF-8 e usam `localhost:6789`. O datagrama máximo
recebido pela implementação tem 1000 bytes.

## Mensagem de dados

Formato: `<ID>,<PAYLOAD>`

Exemplo: `5,Hello`

`ID` é um inteiro decimal positivo. O payload, depois de remover espaços nas
extremidades para validação, não pode estar vazio; pode conter outras vírgulas.
Quando a mensagem fecha a lacuna atual, o servidor devolve como confirmação o
texto completo recebido.

## Pedido de sincronização

Formato exato: `GET_NEXT_ID`

O cliente envia-o ao entrar no modo automático. O pedido não altera o estado do
servidor.

## Resposta de sincronização

Formato: `NEXT_ID,<ID>`

Exemplo: `NEXT_ID,5`

O servidor calcula o valor como `lastDelivered + 1`; não mantém um segundo
contador. O cliente rejeita uma resposta sem o prefixo correto, não numérica ou
com um valor não positivo.

## Mensagem de espera

Formato: `waitingfor,<ID>`

Indica a primeira lacuna da sequência. É enviada quando chega uma mensagem à
frente dessa lacuna ou uma mensagem antiga/duplicada. O cliente automático adota
esse valor como a fonte de verdade. Assim pode avançar quando uma confirmação se
perdeu e recuar quando o servidor foi reiniciado.

## Erro de validação

Formato: `ERROR,INVALID_DATAGRAM`

O servidor devolve este erro para texto desconhecido, ID inválido ou payload
vazio e continua ativo. Exemplos inválidos: `abc`, `1`, `,`, `NEXT_ID,abc`,
`-4,texto` e `5,   `.

## Compatibilidade

O eco existente para uma entrega imediata e `waitingfor` para uma lacuna foram
preservados. O antigo pedido informal `sync` foi substituído pelo comando
explícito `GET_NEXT_ID` e pela resposta não ambígua `NEXT_ID`.
