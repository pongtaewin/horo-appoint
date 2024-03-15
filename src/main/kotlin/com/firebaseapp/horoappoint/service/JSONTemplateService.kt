package com.firebaseapp.horoappoint.service

import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.io.StringWriter

@Service
class JSONTemplateService (
    val templateEngine: SpringTemplateEngine
){
    fun processToString(modelMap: ModelMap, template: String): String{
        return StringWriter().also{
            templateEngine.process(template,Context().apply{setVariables(modelMap)}, it)
        }.toString()

    }
}