package com.firebaseapp.horoappoint.settings

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.templatemode.TemplateMode

@Configuration
class ThymeleafConfiguration {

    @Bean
    fun htmlTessageTemplateResolver(): SpringResourceTemplateResolver {
        return SpringResourceTemplateResolver().apply {
            prefix = "classpath:/templates/"
            resolvablePatterns = setOf("html/*")
            suffix = ".html"
            templateMode = TemplateMode.HTML
            characterEncoding = "UTF-8"
            isCacheable = false
            order = 1
        }

    }


    @Bean
    fun jsonMessageTemplateResolver(): SpringResourceTemplateResolver {
        return SpringResourceTemplateResolver().apply {
            prefix = "classpath:/templates/"
            resolvablePatterns = setOf("json/*")
            suffix = ".json"
            characterEncoding = "UTF-8"
            isCacheable = false
            order = 2
        }
    }


    @Bean
    fun textMessageTemplateResolver(): SpringResourceTemplateResolver {
        return SpringResourceTemplateResolver().apply {
            prefix = "classpath:/templates/"
            resolvablePatterns = setOf("text/*")
            suffix = ".txt"
            templateMode = TemplateMode.TEXT
            characterEncoding = "UTF-8"
            isCacheable = false
            order = 3
        }
    }

    /**
     * Creates the template engine for all message templates.
     *
     * @param templateResolvers Template resolver for different types of messages etc.
     * Note that any template resolvers defined elsewhere will also be included in this
     * collection.
     * todo check running
     * @return Template engine.
     */
    @Bean
    fun messageTemplateEngine(
        templateResolvers: Collection<SpringResourceTemplateResolver>
    ): SpringTemplateEngine {
        return SpringTemplateEngine().apply {
            templateResolvers.forEach(::addTemplateResolver)
        }
    }

}