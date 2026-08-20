package util;

import java.io.IOException;
import java.util.Map;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

public class AuthFilter extends Filter {

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            Http.sendCors(exchange, 204);
            exchange.close();
            return;
        }
        if (path.endsWith("/api/auth/login") || path.endsWith("/api/auth/register")) {
            chain.doFilter(exchange);
            return;
        }

        String userId = TokenService.validate(extractBearer(exchange));
        if (userId == null) {
            Http.sendJson(exchange, 401, Map.of("message", "Não autorizado. Faça login novamente."));
            exchange.close();
            return;
        }

        AuthContext.set(userId);
        try {
            chain.doFilter(exchange);
        } finally {
            AuthContext.clear();
        }
    }

    private String extractBearer(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return header.substring(7).trim();
    }

    @Override
    public String description() {
        return "Valida o token JWT e define o usuário autenticado";
    }
}