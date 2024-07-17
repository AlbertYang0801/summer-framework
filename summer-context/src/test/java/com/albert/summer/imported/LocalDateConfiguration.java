package com.albert.summer.imported;

import com.albert.summer.annotation.Bean;
import com.albert.summer.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Configuration
public class LocalDateConfiguration {

    @Bean
    LocalDate startLocalDate() {
        return LocalDate.now();
    }

    @Bean
    LocalDateTime startLocalDateTime() {
        return LocalDateTime.now();
    }
}
