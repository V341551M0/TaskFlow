package app;

import java.net.BindException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import controller.TaskController;
import service.TaskService;

public class TaskApplication {
    public static void main(String[] args) throws Exception {
        int port = resolvePort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        TaskController controller = new TaskController(new TaskService());
        controller.registerRoutes(server);
        server.setExecutor(null);
        server.start();

        System.out.println("TaskFlow API running at http://localhost:" + port);
    }

    private static int resolvePort() {
        String configuredPort = System.getProperty("server.port", System.getenv().getOrDefault("PORT", "8080"));
        int basePort = Integer.parseInt(configuredPort);

        for (int candidate = basePort; candidate <= basePort + 20; candidate++) {
            try {
                HttpServer tempServer = HttpServer.create(new InetSocketAddress(candidate), 0);
                tempServer.stop(0);
                return candidate;
            } catch (BindException ignored) {
                // tenta a próxima porta
            } catch (Exception ex) {
                throw new RuntimeException("Não foi possível iniciar o servidor HTTP", ex);
            }
        }

        throw new IllegalStateException("Nenhuma porta disponível para a API TaskFlow.");
    }
}