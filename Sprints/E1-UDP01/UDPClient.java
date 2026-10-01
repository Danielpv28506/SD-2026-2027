import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class UDPClient {
    private static final String PALAVRA_SAIDA = "sair";
    private static final String PALAVRA_MODO = "modo";
    private static final String GET_NEXT_ID = "GET_NEXT_ID";
    private static final String NEXT_ID_PREFIX = "NEXT_ID,";
    private static final String WAITING_PREFIX = "waitingfor,";
    private static final int SERVER_PORT = 6789;
    private static final int SOCKET_TIMEOUT_MS = 3000;
    private static final int BUFFER_SIZE = 1000;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket();
             BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in))) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            InetAddress host = InetAddress.getByName("localhost");

            boolean automatic = escolherModo(keyboard);
            Integer nextMessageId = automatic ? obterProximoN(socket, host) : null;
            printInstructions();

            while (true) {
                System.out.print(automatic ? "[auto] > " : "[manual] > ");
                String line = keyboard.readLine();
                if (line == null || line.trim().equalsIgnoreCase(PALAVRA_SAIDA)) {
                    System.out.println("[CLIENT] A terminar o cliente.");
                    break;
                }

                line = line.trim();
                if (line.equalsIgnoreCase(PALAVRA_MODO)) {
                    automatic = escolherModo(keyboard);
                    // Cada entrada no modo automatico substitui o contador local
                    // pelo estado atual do servidor, incluindo apos restart do servidor.
                    nextMessageId = automatic ? obterProximoN(socket, host) : null;
                    continue;
                }
                if (line.isEmpty()) {
                    System.out.println("[CLIENT] Mensagem vazia; nada foi enviado.");
                    continue;
                }

                if (automatic && nextMessageId == null) {
                    nextMessageId = obterProximoN(socket, host);
                    if (nextMessageId == null) {
                        System.out.println("[CLIENT] Nao foi possivel sincronizar; tente novamente.");
                        continue;
                    }
                }

                Integer id = automatic ? nextMessageId : lerNumero(keyboard);
                if (id == null) {
                    continue;
                }

                String sentMessage = id + "," + line;
                if (automatic) {
                    System.out.println("[CLIENT][AUTO] A enviar mensagem ID=" + id);
                }
                String response = sendAndReceive(socket, host, sentMessage);
                if (response != null) {
                    nextMessageId = processResponse(response, sentMessage, automatic,
                            nextMessageId);
                }
            }
        } catch (SocketException e) {
            System.out.println("[CLIENT] Erro de socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("[CLIENT] Erro de I/O: " + e.getMessage());
        }
    }

    private static void printInstructions() {
        System.out.println("Escreva \"" + PALAVRA_SAIDA + "\" para terminar"
                + " ou \"" + PALAVRA_MODO + "\" para trocar de modo.");
    }

    private static boolean escolherModo(BufferedReader keyboard) throws IOException {
        while (true) {
            System.out.print("Modo de numeracao [A]utomatico / [M]anual: ");
            String option = keyboard.readLine();
            if (option == null || option.trim().equalsIgnoreCase("A")) {
                System.out.println("[CLIENT] Modo automatico.");
                return true;
            }
            if (option.trim().equalsIgnoreCase("M")) {
                System.out.println("[CLIENT] Modo manual.");
                return false;
            }
            System.out.println("[CLIENT] Opcao invalida.");
        }
    }

    private static Integer lerNumero(BufferedReader keyboard) throws IOException {
        System.out.print("  numero de sequencia N: ");
        String value = keyboard.readLine();
        if (value == null) {
            System.out.println("[CLIENT] ID em falta; mensagem nao enviada.");
            return null;
        }
        try {
            int id = Integer.parseInt(value.trim());
            if (id <= 0) {
                System.out.println("[CLIENT] O ID tem de ser um inteiro positivo.");
                return null;
            }
            return id;
        } catch (NumberFormatException e) {
            System.out.println("[CLIENT] ID invalido; introduza um numero inteiro positivo.");
            return null;
        }
    }

    private static String sendAndReceive(
            DatagramSocket socket, InetAddress host, String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(data, data.length, host, SERVER_PORT));
        System.out.println("[CLIENT] A enviar: " + message);
        return receive(socket, host);
    }

    private static String receive(DatagramSocket socket, InetAddress host) throws IOException {
        DatagramPacket reply = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
        try {
            socket.receive(reply);
        } catch (SocketTimeoutException e) {
            System.out.println("[CLIENT] Sem resposta do servidor (timeout de "
                    + SOCKET_TIMEOUT_MS + " ms).");
            return null;
        }
        if (!reply.getAddress().equals(host) || reply.getPort() != SERVER_PORT) {
            System.out.println("[CLIENT] Resposta de origem inesperada ignorada.");
            return null;
        }
        return new String(reply.getData(), 0, reply.getLength(), StandardCharsets.UTF_8).trim();
    }

    private static Integer processResponse(String response, String sentMessage,
            boolean automatic, Integer currentNextId) {
        if (response.equals(sentMessage)) {
            System.out.println("[CLIENT] Confirmada pelo servidor: " + response);
            return automatic ? currentNextId + 1 : currentNextId;
        }
        if (response.startsWith(WAITING_PREFIX)) {
            Integer expected = parsePositiveId(response, WAITING_PREFIX);
            if (expected == null) {
                System.out.println("[CLIENT] Resposta waitingfor invalida: " + response);
                return currentNextId;
            }
            System.out.println("[CLIENT] Servidor esta a espera da mensagem " + expected + ".");
            // waitingfor e calculado a partir da fonte de verdade do servidor.
            // Pode avancar apos uma confirmacao perdida ou recuar apos um restart.
            if (automatic) {
                System.out.println("[CLIENT][AUTO] ID local sincronizado para " + expected + ".");
                return expected;
            }
            return currentNextId;
        }
        System.out.println("[CLIENT] Resposta inesperada do servidor: " + response);
        return currentNextId;
    }

    private static Integer obterProximoN(
            DatagramSocket socket, InetAddress host) throws IOException {
        System.out.println("[CLIENT] A sincronizar ID com servidor...");
        byte[] data = GET_NEXT_ID.getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(data, data.length, host, SERVER_PORT));
        String response = receive(socket, host);
        if (response == null) {
            return null;
        }
        Integer nextId = parsePositiveId(response, NEXT_ID_PREFIX);
        if (nextId == null) {
            System.out.println("[CLIENT] Resposta de sincronizacao invalida: " + response);
            return null;
        }
        System.out.println("[CLIENT] Proximo ID recebido: " + nextId);
        return nextId;
    }

    private static Integer parsePositiveId(String message, String prefix) {
        if (!message.startsWith(prefix)) {
            return null;
        }
        try {
            int id = Integer.parseInt(message.substring(prefix.length()).trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
