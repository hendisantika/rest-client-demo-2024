package vn.cloud.restclientdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

public class RetryClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(RetryClientHttpRequestInterceptor.class);

    private int attempts = 3;
    private Set<HttpStatus> retryableStatus = Set.of(HttpStatus.TOO_MANY_REQUESTS);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        for (int i = 0; i < this.attempts; i++) {
            ClientHttpResponse response = execution.execute(request, body);
            if (!retryableStatus.contains(response.getStatusCode())) {
                log.info("Successful at the %d attempts".formatted(i+1));
                return response;
            }

            log.info("%d attempts: %s".formatted(i+1, Instant.now()));
        }
        log.error("Retry exhausted!");
        throw new IllegalStateException("Exceed number of retries");
    }
}
