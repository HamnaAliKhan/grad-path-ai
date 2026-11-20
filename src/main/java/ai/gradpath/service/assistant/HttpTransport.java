package ai.gradpath.service.assistant;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Sends a JSON POST. An interface so tests can replace the network. */
public interface HttpTransport {

    record Response(int status, String body) { }

    Response postJson(String url, Map<String, String> headers, String jsonBody) throws IOException, InterruptedException;

    /** JDK HTTP client with short timeouts. */
    static HttpTransport jdk() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        return (url, headers, body) -> {
            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            headers.forEach(req::header);
            HttpResponse<String> res = client.send(req.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(res.statusCode(), res.body());
        };
    }
}
