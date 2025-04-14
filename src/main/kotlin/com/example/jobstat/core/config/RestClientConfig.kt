// package com.example.jobstat.core.config
//
// import org.springframework.boot.web.client.RestTemplateBuilder
// import org.springframework.context.annotation.Bean
// import org.springframework.context.annotation.Configuration
// import org.springframework.web.client.RestTemplate
// import java.time.Duration
//
// /**
// * REST 클라이언트 설정
// */
// @Configuration
// class RestClientConfig {
//
//    /**
//     * 마이크로서비스 간 통신에 사용되는 RestTemplate 빈을 등록합니다.
//     */
//    @Bean
//    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
//        return builder
//            .setConnectTimeout(Duration.ofSeconds(5))
//            .setReadTimeout(Duration.ofSeconds(5))
//            .build()
//    }
// }
