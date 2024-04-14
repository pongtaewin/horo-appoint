package com.firebaseapp.horoappoint

import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.templatemode.TemplateMode
import java.util.*


fun main(args: Array<String>) {
    runApplication<HoroAppointApplication>(*args)
}


/*
HTML/CSS
Mustache Template

Java
Kotlin
Spring MVC
Spring Boot

Line Bot SDK / Spring Boot
Jackson (JSON)

Firebase Hosting
Google Cloud Storage
Google Cloud SQL
Google Maps API (Geocoding)
 */


@SpringBootApplication
@LineMessageHandler
class HoroAppointApplication {
    // logger
    private val log = LoggerFactory.getLogger(HoroAppointApplication::class.java)


    @Configuration
    @EnableWebSecurity
    class SecurityConfig {

        /**
         * Security Filter Chain
         * 1. Login Page to every requests.
         * 2. Webhook Callback to requests from LINE API endpoints.
         * 3. All pages to every authenicated requests.
         */
        @Bean
        @Throws(Exception::class)
        fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
            http.csrf { it.ignoringRequestMatchers("/callback") }
                .authorizeHttpRequests {
                    it
                        .requestMatchers("/callback").permitAll()
                        .requestMatchers("/login").permitAll()
                        .anyRequest().authenticated()
                }.httpBasic { }
                .formLogin {
                    it.loginPage("/login")
                        //.failureForwardUrl("/staff/login")
                        .permitAll()
                }

            return http.build()
        }

        @Bean
        fun authenticationManager(userDetailsService: UserDetailsService) =
            ProviderManager(DaoAuthenticationProvider().apply {
                setUserDetailsService(userDetailsService)
                setPasswordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder()!!)
            }).apply { isEraseCredentialsAfterAuthentication = false }

        @Bean
        fun userDetailsService() = InMemoryUserDetailsManager(
            User.withUsername("kithisak")
                .password("{bcrypt}\$2a\$10\$qC8xwENeqQYh72oAay0o4ewg7Ff7G3.pQ3urRvsw7IiGnJgcpIFom")
                .build()
        )
    }

    @Configuration
    class ThymeleafConfiguration {
        private companion object {
            private fun baseMTR() = SpringResourceTemplateResolver().apply {
                prefix = "classpath:/templates/"
                characterEncoding = "UTF-8"
                isCacheable = false
            }

            @Bean
            private fun htmlMTR() = baseMTR().apply {
                resolvablePatterns = setOf("html/*")
                suffix = ".html"
                templateMode = TemplateMode.HTML
                order = 1
            }

            @Bean
            private fun jsonMTR() = baseMTR().apply {
                resolvablePatterns = setOf("json/*")
                suffix = ".json"
                order = 2
            }


            @Bean
            private fun textMTR() = baseMTR().apply {
                resolvablePatterns = setOf("text/*")
                suffix = ".txt"
                templateMode = TemplateMode.TEXT
                order = 3
            }
        }

        @Bean
        fun messageTemplateEngine(templateResolvers: Collection<SpringResourceTemplateResolver>) =
            SpringTemplateEngine().apply { templateResolvers.forEach(::addTemplateResolver) }

    }
}

