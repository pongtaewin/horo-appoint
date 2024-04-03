package com.firebaseapp.horoappoint.controller

import com.firebaseapp.horoappoint.settings.UserTab
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping


@Controller
class StaffController {

    @GetMapping("/staff")
    @PostMapping("/staff")
    fun main(model: ModelMap): String {
        val hello = "Hello Pattarachai! สวัสดี!"
        model.addAttribute("testHello", hello)

        model["tabs"] = UserTab.defaultUserTabs
        model["card"] = mapOf(
            "name" to "ธงชัย ใจดี",
            "datetime" to "13 พ.ค. 2567",
            "rows" to listOf("ชื่อ" to "ธงชัย ใจดี", "วันเกิด" to "24 มี.ค. 2523")
        )
        return "html/index"
    }

    @GetMapping("/staff/login")
    fun login(model: ModelMap): String {
        return "html/login"
    }
}



