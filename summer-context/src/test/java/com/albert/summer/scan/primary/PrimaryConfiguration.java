package com.albert.summer.scan.primary;

import com.albert.summer.annotation.Bean;
import com.albert.summer.annotation.Configuration;
import com.albert.summer.annotation.Primary;

@Configuration
public class PrimaryConfiguration {

    @Primary
    @Bean
    DogBean husky() {
        return new DogBean("Husky");
    }

    @Bean
    DogBean teddy() {
        return new DogBean("Teddy");
    }
}
