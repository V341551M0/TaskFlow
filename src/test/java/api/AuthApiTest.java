package api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import controller.AuthController;
import controller.TaskController;
import service.TaskService;
import service.UserService;
import util.AuthFilter;
import util.DatabaseConnection;
import util.Json;
import util.TestDbSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthApiTest {
    private HttpServer server;
    private String baseUrl;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection.initialize();
        TestDbSupport.clearTables();

        server = HttpServer.create(new java.net.InetSocketAddress(0), 0);
        AuthFilter authFilter = new AuthFilter();
        new TaskController(new TaskService()).registerRoutes(server, authFilter);
        new AuthController(new UserService()).registerRoutes(server, authFilter);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldRejectRequestsWithoutToken() throws Exception {
        assertEquals(401, status("GET", "/api/tasks", null));
        assertEquals(401, status("GET", "/api/dashboard", null));
        assertEquals(401, status("GET", "/api/heatmap", null));
    }

    @Test
    void shouldRejectMalformedToken() throws Exception {
        assertEquals(401, status("GET", "/api/tasks", "Bearer nao.eh.um.jwt"));
        assertEquals(401, status("GET", "/api/tasks", "SemPrefixo.tok.en"));
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        long past = (System.currentTimeMillis() / 1000) - 10;
        String expired = buildJwt("usuario-qualquer", past - 3600, past);
        assertEquals(401, status("GET", "/api/tasks", "Bearer " + expired));
    }

    @Test
    void shouldRejectTamperedToken() throws Exception {
        Map<String, Object> user = register("tamper", "tamper@taskflow.test", "senha123");
        String token = (String) user.get("token");
        String[] parts = token.split("\\.");
        String tamperedPayload = new String(
                Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("\"sub\":\"", "\"sub\":\"outro");
        String newPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tamperedPayload.getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + newPayload + "." + parts[2];

        assertEquals(401, status("GET", "/api/tasks", "Bearer " + tampered));
    }

    @Test
    void shouldRegisterLoginAndAccessAuthenticatedEndpoint() throws Exception {
        Map<String, Object> created = register("apiuser", "api@taskflow.test", "senha123");
        assertEquals(201, created.get("__status"));
        String token = (String) created.get("token");
        assertTrue(token != null && !token.isBlank());

        HttpResponse<String> list = send("GET", "/api/tasks", "Bearer " + token, null);
        assertEquals(200, list.statusCode());
        assertEquals("[]", list.body());

        Map<String, Object> data = Json.parseObject(send("POST", "/api/auth/login", null,
                "{\"username\":\"apiuser\",\"password\":\"senha123\"}").body());
        assertEquals("api@taskflow.test", data.get("email"));
        assertTrue(data.containsKey("token"));
    }

    @Test
    void shouldIsolateDataBetweenUsersAtApiLevel() throws Exception {
        Map<String, Object> userA = register("usera", "a@taskflow.test", "senha123");
        Map<String, Object> userB = register("userb", "b@taskflow.test", "senha123");
        String tokenA = (String) userA.get("token");
        String tokenB = (String) userB.get("token");

        HttpResponse<String> created = send("POST", "/api/tasks", "Bearer " + tokenA,
                "{\"nome\":\"Segredo do A\",\"data\":\"2026-08-07\"}");
        assertEquals(201, created.statusCode());
        String taskId = String.valueOf(Json.parseObject(created.body()).get("id"));

        HttpResponse<String> listB = send("GET", "/api/tasks", "Bearer " + tokenB, null);
        assertEquals(200, listB.statusCode());
        assertTrue(!listB.body().contains(taskId));

        HttpResponse<String> completeB = send("POST", "/api/complete", "Bearer " + tokenB,
                "{\"id\":\"" + taskId + "\",\"type\":\"task\",\"date\":\"2026-08-07\"}");
        assertEquals(404, completeB.statusCode());

        HttpResponse<String> deleteB = send("POST", "/api/delete", "Bearer " + tokenB,
                "{\"id\":\"" + taskId + "\",\"type\":\"task\"}");
        assertEquals(404, deleteB.statusCode());
    }

    @Test
    void shouldHaveForeignKeyFromEveryItemTableToUsuario() throws Exception {
        String catalog;
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.createStatement()) {
            catalog = connection.getCatalog();
            var result = statement.executeQuery(
                    "SELECT TABLE_NAME FROM information_schema.REFERENTIAL_CONSTRAINTS "
                            + "WHERE CONSTRAINT_SCHEMA = '" + catalog + "' "
                            + "AND REFERENCED_TABLE_NAME = 'usuario'");
            var tables = new java.util.HashSet<String>();
            while (result.next()) {
                tables.add(result.getString(1));
            }
            assertEquals(5, tables.size(), "todas as tabelas devem referenciar usuario via FK");
            assertTrue(tables.containsAll(List.of("task", "habit", "recurring_task", "item_history", "daily_heatmap")));
        }
    }

    @Test
    void shouldRestrictCorsToAllowedOrigins() throws Exception {
        Map<String, Object> user = register("cors", "cors@taskflow.test", "senha123");
        String token = (String) user.get("token");

        HttpRequest.Builder withOrigin = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/tasks"))
                .header("Authorization", "Bearer " + token)
                .header("Origin", "http://localhost:5501");
        HttpResponse<String> allowed = client.send(withOrigin.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals("http://localhost:5501", allowed.headers().firstValue("Access-Control-Allow-Origin").orElse(null));

        HttpRequest.Builder disallowed = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/tasks"))
                .header("Authorization", "Bearer " + token)
                .header("Origin", "https://malicioso.example");
        HttpResponse<String> blocked = client.send(disallowed.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(null, blocked.headers().firstValue("Access-Control-Allow-Origin").orElse(null));

        HttpResponse<String> noOrigin = send("GET", "/api/tasks", "Bearer " + token, null);
        assertEquals(null, noOrigin.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }

    private Map<String, Object> register(String name, String email, String password) throws Exception {
        HttpResponse<String> response = send("POST", "/api/auth/register", null,
                "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        Map<String, Object> body = Json.parseObject(response.body());
        body.put("__status", response.statusCode());
        return body;
    }

    private int status(String method, String path, String authorization) throws Exception {
        return send(method, path, authorization, null).statusCode();
    }

    private HttpResponse<String> send(String method, String path, String authorization, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10));
        if (body != null) {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String buildJwt(String subject, long iatSeconds, long expSeconds) throws Exception {
        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = b64("{\"sub\":\"" + subject + "\",\"iat\":" + iatSeconds + ",\"exp\":" + expSeconds + "}");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(System.getenv("JWT_SECRET").getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return header + "." + payload + "." + signature;
    }

    private String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}