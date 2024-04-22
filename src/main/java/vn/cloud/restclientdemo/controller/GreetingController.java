package vn.cloud.restclientdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.cloud.restclientdemo.service.HelloServiceClient;

@RestController
public class GreetingController {

    private final HelloServiceClient helloServiceClient;

    public GreetingController(HelloServiceClient helloServiceClient) {
        this.helloServiceClient = helloServiceClient;
    }

    @GetMapping("/greeting")
    String hello() {
        return this.helloServiceClient.hello();
    }

}
