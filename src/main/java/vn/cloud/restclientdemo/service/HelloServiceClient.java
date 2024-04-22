package vn.cloud.restclientdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Service
public class HelloServiceClient {
    private static final Logger log = LoggerFactory.getLogger(HelloServiceClient.class);

    private final RestClient restClient;

    public HelloServiceClient(RestClient.Builder builder) {
        ClientHttpRequestFactorySettings requestFactorySettings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1L))
                .withReadTimeout(Duration.ofSeconds(5L));

        JdkClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(JdkClientHttpRequestFactory.class, requestFactorySettings);

        this.restClient = builder
                .baseUrl("http://localhost:8080")
                .requestFactory(requestFactory)
                .requestInterceptor(
                        (request, body, execution) -> {
                            log.info("Lambda Interceptor: modifying before sending request");
                            ClientHttpResponse response = execution.execute(request, body);
                            log.info("Lambda Interceptor: modifying after receiving response");
                            return response;
                        }
                )
                .requestInterceptor(new TracePropagationClientHttpRequestInterceptor())
                .requestInterceptor(new RetryClientHttpRequestInterceptor())
                .build();
    }

    public String hello() {
        return this.restClient.get()
                .uri("/hello")
                .retrieve()
                .body(String.class);
    }
}
