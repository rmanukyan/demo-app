package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Profile("prod")
public class MyControllerProd {

    @Autowired
    private MyData myData;
    @GetMapping
    public MyData myData() {
        return myData;
    }
}
