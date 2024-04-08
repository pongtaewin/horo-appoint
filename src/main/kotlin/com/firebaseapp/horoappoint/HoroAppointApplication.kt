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

Firebase Authenication
Firebase Hosting
Google Cloud Storage
Google Cloud SQL
LocationIQ
 */


@SpringBootApplication
@LineMessageHandler
class HoroAppointApplication {
    // logger
    private val log = LoggerFactory.getLogger(HoroAppointApplication::class.java)

    /*
    @Autowired
    //@EventListener(ApplicationReadyEvent::class)
    fun test(locationService: LocationService) {
        val (lat, lon) = "13.105607569881593" to "100.91487689675394"
        val result = locationService.retrieveLocationFromAPI(lat, lon)
        if (result == null) log.warn("result is null from lat:$lat, lon:$lon")
        else {
            with(result["address"]) {
                log.info("state: " + get("state"))
                log.info("town: " + get("town"))
                log.info("district: " + get("district"))
                log.info("municipality: " + get("municipality"))
                log.info("postcode: " + get("postcode"))
            }
        }
    }
     */


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
                        .requestMatchers("/staff/login").permitAll()
                        .anyRequest().authenticated()
                }.httpBasic { }.formLogin { it.loginPage("/staff/login").permitAll() }

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

    /* todo temp in HoroAppointApplication

        // Do not call db, bk before initializeFirebase()
        private val db by lazy { FirestoreClient.getFirestore() }
        private val bk by lazy { StorageClient.getInstance().bucket() }
        private val st by lazy { StorageOptions.getDefaultInstance().service }

        @EventListener(ApplicationReadyEvent::class)
        fun initializeFirebase() {
            FirebaseApp.initializeApp(
                FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setProjectId("horo-appoint")
                    .setStorageBucket("horo-appoint.appspot.com")
                    .build()
            )
        }
    */
}

