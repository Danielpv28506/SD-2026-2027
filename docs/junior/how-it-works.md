# Como funciona

O servidor guarda `lastDelivered`, isto é, o último ID entregue sem lacunas. Se
já recebeu e entregou 1, 2, 3 e 4, então:

```text
lastDelivered = 4
```

Se fechares o cliente, o servidor continua a saber isso. Um cliente novo pergunta
`GET_NEXT_ID` (“qual é o próximo ID?”), o servidor responde `NEXT_ID,5`, e o
cliente continua em 5. Não inventa um novo contador a começar em 1.

## Exemplo fora de ordem

```text
1 -> chega -> entrega
3 -> chega -> guarda
4 -> chega -> guarda
2 -> chega -> entrega 2, depois 3, depois 4
```

No fim, `lastDelivered` vale 4 e o buffer volta a estar vazio. Se voltar a chegar
o ID 2, o servidor reconhece que `2 <= 4` e não o entrega outra vez.

## E se algo falhar?

Se o servidor não responder em três segundos, o cliente mostra um timeout e
continua aberto. O ID automático não avança sem confirmação. Se o servidor for
reiniciado, perde o estado (não existe base de dados); uma nova sincronização
dirá ao cliente para recomeçar no ID 1. Se já estava em modo automático, a
resposta que indica que o servidor espera 1 também corrige o contador.
