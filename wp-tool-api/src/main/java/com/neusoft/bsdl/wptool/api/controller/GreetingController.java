package com.neusoft.bsdl.wptool.api.controller;

import com.neusoft.bsdl.wptool.api.dto.GreetingResponse;
import com.neusoft.bsdl.wptool.core.App;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class GreetingController {

    @GetMapping("/greeting")
    public GreetingResponse greeting() {
        App coreApp = new App();
        return new GreetingResponse("wp-tool-api", coreApp.getGreeting());
    }
}
