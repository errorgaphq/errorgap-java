package io.errorgap;

import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FakeIngestor {

    public static final class CapturedRequest {
        public final String method;
        public final String path;
        public final Map<String, String> headers;
        public final String body;

        CapturedRequest(String method, String path, Map<String, String> headers, String body) {
            this.method = method;
            this.path = path;
            this.headers = headers;
            this.body = body;
        }
    }

    private final HttpServer server;
    private final List<CapturedRequest> requests = new ArrayList<>();
    private final int status;
    private final String responseBody;

    public FakeIngestor() throws IOException {
        this(201, "{\"group_id\":\"g_1\"}");
    }

    public FakeIngestor(int status, String responseBody) throws IOException {
        this.status = status;
        this.responseBody = responseBody;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/", exchange -> {
            byte[] requestBody = readAll(exchange.getRequestBody());
            Map<String, String> hdrs = new HashMap<>();
            exchange.getRequestHeaders().forEach((k, vs) -> {
                if (!vs.isEmpty()) hdrs.put(k.toLowerCase(), vs.get(0));
            });
            synchronized (requests) {
                requests.add(new CapturedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    hdrs,
                    new String(requestBody)
                ));
            }
            byte[] resp = responseBody.getBytes();
            exchange.getResponseHeaders().add("content-type", "application/json");
            exchange.sendResponseHeaders(status, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        this.server.start();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    public String endpoint() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public List<CapturedRequest> requests() {
        synchronized (requests) {
            return new ArrayList<>(requests);
        }
    }

    public void close() {
        server.stop(0);
    }
}
