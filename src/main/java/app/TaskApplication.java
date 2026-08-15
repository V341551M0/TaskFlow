package app;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import controller.AuthController;
import controller.TaskController;
import service.TaskService;
import service.UserService;

public class TaskApplication {
    public static void main(String[] args) throws Exception {
        String configuredPort = System.getProperty("server.port", System.getenv().getOrDefault("PORT", "8080"));
        int basePort = Integer.parseInt(configuredPort);

        HttpServer server = startServer(basePort);
        TaskController controller = new TaskController(new TaskService());
        AuthController authController = new AuthController(new UserService());
        controller.registerRoutes(server);
        authController.registerRoutes(server);
        server.setExecutor(null);
        server.start();

        System.out.println("TaskFlow API running at http://localhost:" + server.getAddress().getPort());
    }

    private static HttpServer startServer(int basePort) throws IOException {
        for (int candidate = basePort; candidate <= basePort + 20; candidate++) {
            try {
                return HttpServer.create(new InetSocketAddress(candidate), 0);
            } catch (BindException ignored) {
                // tenta a próxima porta
            }
        }
        throw new IllegalStateException("Nenhuma porta disponível para a API TaskFlow.");
    }
}
