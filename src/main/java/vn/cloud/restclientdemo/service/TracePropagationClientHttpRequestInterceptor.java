package vn.cloud.restclientdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.UUID;

public class TracePropagationClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
    private final Logger log = LoggerFactory.getLogger(TracePropagationClientHttpRequestInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        log.info("TracePropagationClientHttpRequest Interceptor: modifying before sending request");
        HttpHeaders headers = request.getHeaders();
        headers.set("trace_id", UUID.randomUUID().toString());
        headers.set("span_id", UUID.randomUUID().toString());

        ClientHttpResponse response = execution.execute(request, body);

        log.info("TracePropagationClientHttpRequest Interceptor: modifying after receiving response");
        return response;
    }
}
