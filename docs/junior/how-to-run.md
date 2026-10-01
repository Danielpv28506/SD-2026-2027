# Como executar

É necessário ter um JDK instalado. A partir da raiz do repositório:

```bash
cd Sprints/E1-UDP01
javac UDPClient.java UDPServer.java
```

## 1. Iniciar o servidor

No primeiro terminal:

```bash
java UDPServer
```

O log indica que está a ouvir no porto 6789 e que `lastDelivered=0`.

## 2. Iniciar o cliente

Num segundo terminal, na mesma pasta:

```bash
java UDPClient
```

Escolhe `A` (automático) ou `M` (manual). Em automático, escreve apenas o texto:
o cliente consulta o próximo ID e mostra o datagrama enviado. Em manual, escreve
o texto e depois o ID positivo pedido. Usa `modo` para trocar de modo e `sair`
para fechar.

Os logs `entregue`, `fora de ordem`, `a espera` e `duplicada` explicam o que o
servidor fez. `waitingfor,2`, por exemplo, significa que falta o ID 2.

## 3. Testar reinícios

- **Cliente:** depois de entregar algumas mensagens, escreve `sair`, executa
  `java UDPClient` novamente e escolhe `A`. O próximo ID mostrado deve continuar
  a sequência do servidor.
- **Servidor:** para o servidor com `Ctrl+C`, executa `java UDPServer` novamente
  e, no cliente, usa `modo` e escolhe `A`. Sem persistência, a sincronização
  mostrará o ID 1.

Para testar uma lacuna, escolhe manual e envia os IDs 1, 3, 4 e 2. Para limpar os
ficheiros compilados no fim:

```bash
rm -f *.class
```
