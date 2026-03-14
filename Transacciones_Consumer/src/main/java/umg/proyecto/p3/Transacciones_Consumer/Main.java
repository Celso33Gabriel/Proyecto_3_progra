package umg.proyecto.p3.Transacciones_Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.universidad.proyecto.model.Transaccion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class Main {

    private static final String POST_URL = "https://7e0d9ogwzd.execute-api.us-east-1.amazonaws.com/default/guardarTransacciones";
    private static final String[] BANCOS = {"BAC", "BANRURAL", "BI", "GYT"};
    // Nombre de la nueva cola para la Serie II
    private static final String COLA_RECHAZADOS = "cola_rechazados";

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); 

        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            ObjectMapper mapper = new ObjectMapper();

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            // --- MODIFICACIÓN SERIE II: Declarar la cola de rechazados ---
            channel.queueDeclare(COLA_RECHAZADOS, true, false, false, null);

            System.out.println(" [*] Esperando transacciones. Filtro: > Q4000 será RECHAZADA. CTRL+C para salir.");

            for (String banco : BANCOS) {
                channel.queueDeclare(banco, true, false, false, null);

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String jsonMensaje = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    
                    try {
                        Transaccion tx = mapper.readValue(jsonMensaje, Transaccion.class);
                        
                        // --- LÓGICA DE FILTRADO SERIE II ---
                        System.out.println("\n=========================================");
                        System.out.println("ID Transacción: " + tx.getIdTransaccion());
                        System.out.println("Monto: Q" + tx.getMonto());
                        System.out.println("Banco Origen: " + banco);

                        if (tx.getMonto() > 4000.00) {
                            // CASO: RECHAZADA
                            System.out.println("ESTADO: RECHAZADA");
                            System.out.println("Motivo: El monto excede el límite de Q4000.00");
                            
                            // Enviamos a la cola de rechazados
                            channel.basicPublish("", COLA_RECHAZADOS, null, jsonMensaje.getBytes(StandardCharsets.UTF_8));
                            
                            // Confirmamos a Rabbit que el mensaje fue "atendido" (movido de cola)
                            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                            System.out.println("Resultado: Movido a 'cola_rechazados'");

                        } else {
                            // CASO: ACEPTADA (Flujo original a Amazon)
                            System.out.println("ESTADO: ACEPTADA");
                            
                            boolean exitoso = enviarACloud(httpClient, jsonMensaje);

                            if (exitoso) {
                                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                                System.out.println("Resultado: Guardado en Amazon y confirmado.");
                            } else {
                                System.err.println(" [❌] Error al enviar a Amazon. Reintentando...");
                                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                            }
                        }
                        System.out.println("=========================================");

                    } catch (Exception e) {
                        System.err.println(" [!] Error procesando el mensaje: " + e.getMessage());
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                    }
                };

                channel.basicConsume(banco, false, deliverCallback, consumerTag -> {});
            }

        } catch (Exception e) {
            System.err.println("Error en el Consumer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean enviarACloud(HttpClient client, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(POST_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("   Respuesta Amazon: " + response.statusCode() + " - " + response.body());

            return response.statusCode() == 200 || response.statusCode() == 201;

        } catch (Exception e) {
            System.err.println("   Fallo conexión Amazon: " + e.getMessage());
            return false;
        }
    }
}