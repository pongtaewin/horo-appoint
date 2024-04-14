package com.firebaseapp.horoappoint.controller

import com.firebaseapp.horoappoint.entity.Appointment
import com.firebaseapp.horoappoint.repository.AppointmentRepository
import com.firebaseapp.horoappoint.repository.TimeframeRepository
import com.firebaseapp.horoappoint.service.MessageService
import com.firebaseapp.horoappoint.settings.ThaiFormatter
import com.linecorp.bot.messaging.model.TextMessage
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam


@Controller
class StaffController(
    val appointmentRepository: AppointmentRepository,
    val timeframeRepository: TimeframeRepository,
    val messageService: MessageService
) {

    @GetMapping("/")
    @PostMapping("/")
    fun main(
        model: ModelMap,
        @RequestParam("search") search: String?,
        @RequestParam("filter") filter: String?,
        @RequestParam("selected") selected: String?,
        @RequestParam("action") action: String?
    ): String {

        if (action == "confirm") {
            val now = ThaiFormatter.now().toInstant()
            val i = selected!!.toLong()
            appointmentRepository.save(appointmentRepository.findById(i).get().apply {
                approved = now
                timeframeRepository.save(timeframe!!.apply { approved = true })
            }).also {
                messageService.pushMessage(
                    it.customer!!,
                    TextMessage("อาจารย์ได้อนุมัติเรียบร้อยแล้ว\nอีกสักครู่อาจารย์จะทำการสอบถามข้อมูลจากท่าน\nขอขอบพระคุณที่ใช้งานระบบจองครับ")
                )
            }

        }

        val hello = "Hello Pattarachai! สวัสดี!"
        model.addAttribute("testHello", hello)

        val filters = filterByOption(filter)
        val results = appointmentRepository.findByMatchingQueryOnCustomerOrService(search ?: "").filter(filters)
        val selection =
            selected?.toLong() ?: if (results.isNotEmpty()) results[0].id!! else -1L

        model["search"] = search
        model["filter"] = filter ?: "all"
        model["selected"] = selection
        model["page"] = mapOf("title" to "แอปพลิเคชัน HoroAppoint")
        model["tabs"] = results.map {
            mapOf(
                "id" to it.id!!,
                "picked" to if (it.id!! == selection) "true" else "false",
                "name" to it.customer!!.displayName,
                "subject" to it.serviceChoice!!.service!!.name!!,
                "desc" to buildString {
                    append(it.serviceChoice!!.name!! + "<br>")
                    if (it.timeframe != null) {
                        append(it.timeframe!!.getCombinedDate() + "<br>")
                        append(it.timeframe!!.getCombinedTime() + "<br>")
                    }
                    append(it.getLocationDescriptor().replace("\\n", "<br>"))
                },
                "src" to it.customer!!.displayImage!!.toString(),
                "selected" to (it.id!! == selection && selection != -1L),
                "color" to appointmentColor(appointmentType(it))
            )
        }
        val ap = appointmentRepository.findByIdOrNull(selection)
        if (ap != null) model["card"] = mapOf(
            "id" to ap.id!!,
            "name" to ap.customer!!.displayName,
            "fullName" to ap.customer!!.fullName,
            "service" to ap.serviceChoice!!.service!!.name!!,
            "choice" to ap.serviceChoice!!.name!!,
            "location" to ap.getLocationDescriptor().replace("\\n", " "),
            "date" to ap.timeframe!!.getCombinedDate(),
            "time" to ap.timeframe!!.getCombinedTime(),
            "src" to ap.customer!!.displayImage!!.toString(),
            "slip" to ap.slipImage?.toString(),
            "slipFinal" to (ap.slipFinal?: ap.slipAdded)?.let { ThaiFormatter.format(ThaiFormatter.asZone(it), "D MMM yyyy hh:mm:ss") },
            "color" to appointmentColor(appointmentType(ap))
        )
        return "html/index"
    }

    fun appointmentColor(type: String): String = when (type) {
        "finished" -> "#404040"
        "ready" -> "#40c365"
        "confirmation" -> "#ffc94c"
        "payment" -> "#41ccb4"
        "booking" -> "#298071"
        else -> "#ffffff"
    }

    fun appointmentType(appointment: Appointment) = with(appointment) {
        when {
            finished != null -> "finished"
            approved != null -> "ready"
            slipFinal != null -> "confirmation"
            selectionFinal != null -> "payment"
            selectionAdded != null -> "booking"
            else -> "none"
        }
    }

    fun filterByOption(filter: String?): (Appointment) -> Boolean = when (filter) {
        "finished", "ready", "confirmation", "payment" -> {
            { appointmentType(it) == filter }
        }

        else -> {
            { true }
        }
    }

    @GetMapping("/login")
    @PostMapping("/login")
    fun login(model: ModelMap): String {
        return "html/login"
    }
}
