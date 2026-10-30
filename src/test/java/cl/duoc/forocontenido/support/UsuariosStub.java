package cl.duoc.forocontenido.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public final class UsuariosStub implements AutoCloseable {
    public static final String USUARIO = "{\"id\":7,\"nombre\":\"Ana\",\"correo\":\"ana@example.test\",\"rol\":\"USUARIO\"}";
    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    public final AtomicInteger peticiones = new AtomicInteger();
    public final AtomicInteger redirecciones = new AtomicInteger();
    public volatile String authorization;
    public volatile String cookie;
    private volatile Respuesta respuesta = new Respuesta(200, USUARIO, "application/json", 0, 0, false);

    public UsuariosStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/api/auth/me", this::responder);
        server.createContext("/destino-prohibido", exchange -> {
            redirecciones.incrementAndGet();
            enviar(exchange, new Respuesta(200, USUARIO, "application/json", 0, 0, false));
        });
        server.start();
    }

    public String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort(); }

    public void reset() {
        respuesta = new Respuesta(200, USUARIO, "application/json", 0, 0, false);
        peticiones.set(0);
        redirecciones.set(0);
        authorization = null;
        cookie = null;
    }

    public void respuesta(int status, String body) {
        respuesta = new Respuesta(status, body, "application/json", 0, 0, false);
    }

    public void tipo(String tipo) { respuesta = new Respuesta(200, USUARIO, tipo, 0, 0, false); }
    public void demora(boolean cuerpo) {
        respuesta = new Respuesta(200, USUARIO, "application/json", cuerpo ? 0 : 1500, cuerpo ? 1500 : 0, false);
    }
    public void desconectar() { respuesta = new Respuesta(200, "", "application/json", 0, 0, true); }

    private void responder(HttpExchange exchange) throws IOException {
        peticiones.incrementAndGet();
        authorization = exchange.getRequestHeaders().getFirst("Authorization");
        cookie = exchange.getRequestHeaders().getFirst("Cookie");
        enviar(exchange, respuesta);
    }

    private void enviar(HttpExchange exchange, Respuesta value) throws IOException {
        try {
            if (value.desconectar) return;
            if (value.antes > 0) Thread.sleep(value.antes);
            exchange.getResponseHeaders().set("Content-Type", value.tipo);
            if (value.status == 302) exchange.getResponseHeaders().set("Location", baseUrl() + "/destino-prohibido");
            byte[] bytes = value.body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(value.status, value.status == 204 ? -1 : bytes.length);
            if (value.despues > 0) Thread.sleep(value.despues);
            if (value.status != 204) exchange.getResponseBody().write(bytes);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            // El timeout del cliente cierra la conexión deliberadamente en varias pruebas.
        } finally {
            exchange.close();
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private record Respuesta(int status, String body, String tipo, long antes, long despues, boolean desconectar) { }
}
