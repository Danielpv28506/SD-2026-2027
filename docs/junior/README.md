# Cliente/servidor UDP — guia simples

Este projeto mostra dois programas Java a trocar pequenas mensagens pela rede.
O `UDPClient` pergunta ao utilizador o que enviar. O `UDPServer` recebe, valida e
entrega as mensagens pela ordem dos seus IDs.

Um ID funciona como o número de uma página: 1 vem antes de 2, e 2 antes de 3.
UDP pode fazer a “página 3” chegar antes da “página 2”. Por isso, o servidor guarda
temporariamente a 3 e espera pela 2. Sem IDs, não conseguiria reconstruir a ordem.

Há dois modos:

- **manual:** escolhes o texto e o ID;
- **automático:** escreves o texto e o cliente obtém/avança o ID por ti.

O servidor é quem sabe qual é o próximo ID correto. Isto permite fechar e voltar
a abrir o cliente sem começar novamente no 1.
