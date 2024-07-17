package com.albert.summer.imported;

import com.albert.summer.annotation.Bean;
import com.albert.summer.annotation.Configuration;

import java.time.ZonedDateTime;


@Configuration
public class ZonedDateConfiguration {

    @Bean
    ZonedDateTime startZonedDateTime() {
        return ZonedDateTime.now();
    }
}
