package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Component
public class MyData {
    @Value("${mydata.secret.one}")
    private int field1;

    @Value("${mydata.secret.two}")
    private String field2;

    @Value("${mydata.secret.three}")
    private String field3;
}
