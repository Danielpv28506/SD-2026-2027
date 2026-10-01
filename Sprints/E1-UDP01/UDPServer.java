import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UDPServer {
    private static final int SERVER_PORT = 6789;
    private static final int BUFFER_SIZE = 1000;
    private static final String GET_NEXT_ID = "GET_NEXT_ID";

    private static final List<String> mensagensEntregues = new ArrayList<>();
    private static final Map<Integer, String> mensagensTemporarias = new HashMap<>();

    /**
     * Entrega a mensagem atual e desbloqueia, por ordem, as que estavam no buffer.
     * O valor devolvido e a unica origem do estado lastDelivered mantido no main.
     */
    public static int processDeliveredMessages(
            int lastDelivered, int currentId, String currentMessage) {
        if (currentId <= lastDelivered) {
            return lastDelivered;
        }

        if (currentId == lastDelivered + 1) {
            deliver(currentId, currentMessage, false);
            int delivered = currentId;
            while (mensagensTemporarias.containsKey(delivered + 1)) {
                delivered++;
                deliver(delivered, mensagensTemporarias.remove(delivered), true);
            }
            return delivered;
        }

        // putIfAbsent impede um duplicado fora de ordem de substituir o original.
        mensagensTemporarias.putIfAbsent(currentId, currentMessage);
        return lastDelivered;
    }

    private static void deliver(int id, String message, boolean unblocked) {
        mensagensEntregues.add(message);
        String detail = unblocked ? " desbloqueada e entregue." : " entregue.";
        System.out.println("[SERVER] Mensagem " + id + detail);
    }

    public static void main(String[] args) {
        int lastDelivered = 0;

        try (DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {
            System.out.println("[SERVER] UDP a escutar no porto " + SERVER_PORT + ".");
            System.out.println("[SERVER] Estado inicial: lastDelivered=" + lastDelivered);

            while (true) {
                DatagramPacket request = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
                socket.receive(request);
                String text = new String(request.getData(), 0, request.getLength(),
                        StandardCharsets.UTF_8).trim();

                if (GET_NEXT_ID.equals(text)) {
                    int nextId = lastDelivered + 1;
                    System.out.println("[SERVER] Cliente solicitou NEXT_ID.");
                    System.out.println("[SERVER] lastDelivered=" + lastDelivered
                            + " -> nextId=" + nextId);
                    send(socket, request, "NEXT_ID," + nextId);
                    continue;
                }

                ParsedMessage message = parseDataMessage(text);
                if (message == null) {
                    System.out.println("[SERVER] Datagrama invalido ignorado: \"" + text + "\"");
                    send(socket, request, "ERROR,INVALID_DATAGRAM");
                    continue;
                }

                int previousLastDelivered = lastDelivered;
                System.out.println("[SERVER] Mensagem " + message.id + " recebida.");
                lastDelivered = processDeliveredMessages(
                        lastDelivered, message.id, message.originalText);

                if (message.id <= previousLastDelivered) {
                    System.out.println("[SERVER] Mensagem " + message.id
                            + " antiga/duplicada; nao sera entregue novamente.");
                } else if (message.id > previousLastDelivered + 1) {
                    System.out.println("[SERVER] Mensagem " + message.id
                            + " recebida fora de ordem.");
                    System.out.println("[SERVER] A espera da mensagem "
                            + (previousLastDelivered + 1) + ".");
                }

                String response = message.id == previousLastDelivered + 1
                        ? message.originalText
                        : "waitingfor," + (lastDelivered + 1);
                send(socket, request, response);
            }
        } catch (SocketException e) {
            System.out.println("[SERVER] Erro de socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("[SERVER] Erro de I/O: " + e.getMessage());
        }
    }

    private static ParsedMessage parseDataMessage(String text) {
        int comma = text.indexOf(',');
        if (comma <= 0 || comma == text.length() - 1) {
            return null;
        }

        String payload = text.substring(comma + 1).trim();
        if (payload.isEmpty()) {
            return null;
        }

        try {
            int id = Integer.parseInt(text.substring(0, comma).trim());
            return id > 0 ? new ParsedMessage(id, text) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void send(DatagramSocket socket, DatagramPacket request, String text)
            throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        DatagramPacket reply = new DatagramPacket(
                data, data.length, request.getAddress(), request.getPort());
        socket.send(reply);
    }

    private static final class ParsedMessage {
        private final int id;
        private final String originalText;

        private ParsedMessage(int id, String originalText) {
            this.id = id;
            this.originalText = originalText;
        }
    }
}
