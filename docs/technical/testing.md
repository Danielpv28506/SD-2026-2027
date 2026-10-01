# Testes

Os testes foram executados com `javac`, os dois programas Java e um pequeno
cliente UDP Python para fazer asserções exatas ao protocolo. O estado do servidor
é apenas em memória.

## 1 — Execução normal

- **Objetivo:** confirmar sequência automática inicial.
- **Passos:** iniciar servidor; pedir `GET_NEXT_ID`; enviar IDs 1, 2, 3 e 4.
- **Esperado:** sincronização em 1 e entrega 1–4.
- **Obtido:** passou; cada mensagem foi ecoada e `NEXT_ID,5` foi devolvido no fim.

## 2 — Reiniciar apenas o cliente

- **Objetivo:** não regressar ao ID 1.
- **Passos:** manter o servidor depois da entrega até 4; criar um novo socket
  cliente e enviar `GET_NEXT_ID`.
- **Esperado:** `NEXT_ID,5` e próximo envio com ID 5.
- **Obtido:** passou; a resposta observada foi `NEXT_ID,5`.

## 3 — Reiniciar servidor

- **Objetivo:** confirmar que o cliente pode adotar o estado reiniciado.
- **Passos:** parar e voltar a iniciar `UDPServer`; entrar/reentrar no modo
  automático, que envia uma nova sincronização.
- **Esperado:** como não há persistência, `NEXT_ID,1`.
- **Obtido:** passou; um processo novo respondeu `NEXT_ID,1`.

## 4 — Mensagem fora de ordem

- **Objetivo:** validar o buffer e o fecho de lacunas.
- **Passos:** enviar `1,A`, `3,C`, `4,D`, `2,B`.
- **Esperado:** entregar 1; guardar 3 e 4; entregar 2, 3 e 4 ao chegar 2.
- **Obtido:** passou; os logs mostraram 3 e 4 como desbloqueadas e o próximo ID 5.

## 5 — Duplicado

- **Objetivo:** impedir uma segunda entrega.
- **Passos:** com `lastDelivered=4`, enviar `2,duplicate`.
- **Esperado:** não entregar; responder que espera 5.
- **Obtido:** passou; log de antiga/duplicada e resposta `waitingfor,5`.

## 6 — Cliente reiniciado várias vezes

- **Objetivo:** confirmar sincronizações sucessivas.
- **Passos:** entregar até 8; novo cliente consulta e envia 9; outro novo cliente
  consulta novamente.
- **Esperado:** primeiro recebe 9 e o segundo recebe 10.
- **Obtido:** passou no ensaio do protocolo com sockets novos: `NEXT_ID,9`, entrega
  de 9 e depois `NEXT_ID,10`.

## 7 — Servidor indisponível

- **Objetivo:** evitar bloqueio ou stack trace no cliente.
- **Passos:** sem servidor, executar o cliente, escolher automático e aguardar.
- **Esperado:** mensagem compreensível após cerca de 3000 ms e processo apto a
  tentar novamente.
- **Obtido:** passou; foi mostrado `Sem resposta do servidor (timeout de 3000 ms)`
  e não ocorreu exceção não tratada.

## 8 — Datagramas inválidos

- **Objetivo:** validar entrada hostil sem parar o servidor.
- **Passos:** enviar `abc`, `1`, `,`, `NEXT_ID,abc`, `-4,texto` e `5,   `; depois
  enviar `GET_NEXT_ID`.
- **Esperado:** erro para cada inválido e servidor ainda disponível.
- **Obtido:** passou; todos receberam `ERROR,INVALID_DATAGRAM` e a consulta
  seguinte recebeu uma resposta válida.

## Comandos de verificação

```bash
cd Sprints/E1-UDP01
javac UDPClient.java UDPServer.java
java UDPServer
# Noutro terminal:
java UDPClient
```
