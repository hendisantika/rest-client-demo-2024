# RestClient in Spring Boot 3 - Builder, Timeout, Interceptor, RequestFactory
> This repo is used in this Youtube video: https://youtu.be/iNWVlF8o0A4

## 1. Practice when using RestClient
> **Noted:** RestClient is thread-safe

### ~~1.1. Create one instance inside every request-call method~~ 
```java
@Service
public class HelloServiceClient {

    public String getHelloById() {
        RestClient restClient = RestClient.create(); <-- 1 instance will be created for every method call
        this.restClient.get()...
        ...
    }
    
    public List<String> getHellos() {
        RestClient restClient = RestClient.create();
        this.restClient.get()...
        ...
    }
}
```

### 1.2. Create only one instance for a entire service
```java
@Configuration
public class WebConfig {

    @Bean
    public RestClient restClient() {
       return RestClient.create();
    }
}
```
```java
@Service
public class HelloServiceClient {
    
    private final RestClient restClient;

    public HelloServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public String getHelloById() {
        this.restClient.get()...
        ...
    }


    public List<String> getHellos() {
        this.restClient.get()...
        ...
    }
}
```

### 1.3. One instance per one ServiceClient Class
```java
@Service
public class HelloServiceClient {
    
    private final RestClient restClient;

    public HelloServiceClient() {
        this.restClient = RestClient.builder()
                .baseUrl("http://helloservice.com")
                .requestInterceptor(new ClientCredentialTokenForHelloServiceInterceptor())
                .build();
    }

    public String getHelloById() {
        this.restClient.get()...
        ...
    }


    public List<String> getHellos() {
        this.restClient.get()...
        ...
    }
}
```
```java
@Service
public class HiServiceClient {
    
    private final RestClient restClient;

    public HiServiceClient() {
        this.restClient = RestClient.builder()
                .baseUrl("http://hiservice.com")
                .requestInterceptor(new ClientCredentialTokenForHiServiceInterceptor())
                .build();
    }

    public String getHiById() {
        this.restClient.get()...
        ...
    }


    public List<String> getHis() {
        this.restClient.get()...
        ...
    }
}
```

## 2. Create a RestClient

### 2.1. In [Spring Framework](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#_creating_a_restclient), from static methods of RestClient: `create`, `builder`
```java
RestClient defaultClient = RestClient.create();

RestClient customClient = RestClient.builder()
        .requestFactory(new HttpComponentsClientHttpRequestFactory())
        .messageConverters(converters -> converters.add(new MyCustomMessageConverter()))
        .baseUrl("https://example.com")
        .defaultUriVariables(Map.of("variable", "foo"))
        .defaultHeader("My-Header", "Foo")
        .requestInterceptor(myCustomInterceptor)
        .requestInitializer(myCustomInitializer)
        .build();
```

### 2.2. [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#io.rest-client.restclient) adds 1 additional way is from auto-configured `RestClient.Builder` prototype bean in RestClientAutoConfiguration class
```java
@Service
public class HelloServiceClient {

    private final RestClient restClient;

    public HelloServiceClient(RestClient.Builder builder) { <-- RestClient.Builder bean is autowired here
        this.restClient = builder
                .baseUrl("http://helloservice.com")
                .requestInterceptor(new ClientCredentialTokenForHelloServiceInterceptor())
                .build();
    }

    public String getHelloById() {
        this.restClient.get()...
        ...
    }
}
```
> **Noted:** RestClient.Builder is prototype bean, 1 new instance of it will be created for each autowiring.

## 3. Set Connection Timeout (connectTimeout), Response Timeout (readTimeout), and RequestFactory

In order to set timeouts to our outgoing requests from a `RestClient`, we have to set them through the `ClientHttpRequestFactory` of this `RestClient`.

But each type of `ClientHttpRequestFactory` has it own structure and they differ from others so we have to know the configuration of the underlying components to configure it right.

**For example:** 
* `SimpleClientHttpRequestFactory`: we can set both _connection timeout_ and _response timeout_ on this `SimpleClientHttpRequestFactory` itself
```java
SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
simpleClientHttpRequestFactory.setConnectTimeout(Duration.ofSeconds(1L)); <--
simpleClientHttpRequestFactory.setReadTimeout(Duration.ofSeconds(5L)); <--

this.restClient = RestClient.builder()
        .requestFactory(simpleClientHttpRequestFactory)
        .build();
```
* `JdkClientHttpRequestFactory`: the _connection timeout_ is set on HttpClient that is passed into `JdkClientHttpRequestFactory`'s constructor
and the `response timeout` is set on the `JdkClientHttpRequestFactory`
```java
HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1L)).build(); <--
JdkClientHttpRequestFactory jdkClientHttpRequestFactory = new JdkClientHttpRequestFactory(httpClient);
jdkClientHttpRequestFactory.setReadTimeout(Duration.ofSeconds(5L)); <--

this.restClient = RestClient.builder()
        .requestFactory(jdkClientHttpRequestFactory)
        .build();
```

Spring simplified the configuration of underlying components by `ClientHttpRequestFactorySettings` and `ClientHttpRequestFactories`:
```java
ClientHttpRequestFactorySettings requestFactorySettings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1L))
                .withReadTimeout(Duration.ofSeconds(5L));

JdkClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(JdkClientHttpRequestFactory.class, requestFactorySettings);

this.restClient = RestClient.builder()
        .requestFactory(requestFactory)
        .build();
```

## 4.RestClient Interceptor

### 4.1. Defined as a Lambda Expression
RequestInterceptor is a `FunctionalInterface`, so we can pass `a lambda expression` to it
```java
this.restClient = RestClient.builder()
        .baseUrl("http://localhost:8080")
        .requestFactory(requestFactory)
        .requestInterceptor(
                /////// <--
                (request, body, execution) -> {
                        log.info("Lambda Interceptor: modifying before sending request");
                        ClientHttpResponse response = execution.execute(request, body);
                        log.info("Lambda Interceptor: modifying after receiving response");
                        return response;
                }
                ///////
        )
        .build();
```
> **Noted:** they work exactly same as Filters, we can modify the request before executing the chain, 
> response after the chain returned and if we're not satisfied with response, we can execute the chain again

### 4.2. Defined as a Class
```java
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
```

```java
this.restClient = RestClient.builder()
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
                .build();
```

> **Noted:** the order is matter (like Filters as well)

                                                     request                 response
    1. Lambda Interceptor: before sending request       |                       ^
                                                        v                       |      4. Lambda Interceptor: after receiving response
                                            +----------------------------------------------+
                                            |               Lambda Interceptor             |
                                            +----------------------------------------------+
    2. Trace Interceptor: before sending request        |                       ^
                                                        v                       |      3. Trace Interceptor: after receiving response
                                            +----------------------------------------------+
                                            | TracePropagationClientHttpRequestInterceptor |
                                            +----------------------------------------------+
                                                        |                       ^
                                                        v                       |
                                            +----------------------------------------------+
                                            |               External Service               |
                                            +----------------------------------------------+

### 4.3. Simple Retry Interceptor
We can add retry on a Interceptor as well, for example, we're going to retry when receiving 429 Too Many Requests from external service.
```java
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
```
## References
- `1.` **RestClient** in [**Spring Framework 6**](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#_creating_a_restclient).
- `2.` **RestClient** in [**Spring Boot 3**](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#io.rest-client.restclient) 
