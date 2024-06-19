package com.firebaseapp.horoappoint.controller

import com.firebaseapp.horoappoint.HoroAppointApplication.Companion.getImg
import com.firebaseapp.horoappoint.ThaiFormatter
import com.firebaseapp.horoappoint.entity.Appointment
import com.firebaseapp.horoappoint.entity.Timeframe
import com.firebaseapp.horoappoint.repository.AppointmentRepository
import com.firebaseapp.horoappoint.repository.TimeframeRepository
import com.firebaseapp.horoappoint.service.PaymentService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*

private const val PAGE_TITLE = "แอปพลิเคชัน HoroAppoint"
private const val DATE_FORMAT = "d MMM yy HH:mm:ss"
private const val MONTHLY_DISPLAY_DAYS = 42
private const val MAX_YEARS_BACKLOG = 5
private const val MONTHS_OF_YEAR = 12

@Controller
@Suppress("TooManyFunctions", "SameReturnValue", "SameReturnValue")
class StaffController(
    val appointmentRepository: AppointmentRepository,
    val timeframeRepository: TimeframeRepository,
    private val paymentService: PaymentService
) {
    fun confirmedMain(model: ModelMap, params: Map<String, String>): String {
        val now = ThaiFormatter.now().toInstant()
        val i = (params["selected"] ?: error("Required attribute 'selected'")).toLong()
        paymentService.handleSuccessEvent(
            appointmentRepository.save(
                appointmentRepository.findById(i).get().apply {
                    approved = now
                    timeframeRepository.save(timeframe!!.apply { approved = true })
                }
            )
        )
        return main(model, params["search"], params["filter"], params["selected"], params["selectedNew"], null)
    }

    private val appointmentColor: Map<String, String> = mapOf(
        "finished" to "#404040",
        "ready" to "#40c365",
        "confirmation" to "#ffc94c",
        "payment" to "#41ccb4",
        "booking" to "#298071"
    )

    @Suppress("LongMethod", "LongParameterList")
    @GetMapping("/")
    @PostMapping("/")
    fun main(
        model: ModelMap,
        @RequestParam("search") search: String?,
        @RequestParam("filter") filter: String?,
        @RequestParam("selected") selected: String?,
        @RequestParam("selectedNew") selectedNew: String?,
        @RequestParam("action") action: String?
    ): String {
        val sel = selectedNew ?: selected
        if (action == "confirm") {
            return confirmedMain(
                model,
                mapOf("search" to search, "filter" to filter, "selected" to selected, "selectedNew" to selectedNew)
                    .filterValues { it != null }.mapValues { it.value!! }
            )
        }

        val filters = filterByOption(filter)
        val results = appointmentRepository.findByMatchingQueryOnCustomerOrService(search ?: "").filter(filters)
        val selection = sel?.toLong() ?: if (results.isNotEmpty()) results[0].id!! else -1L

        model["search"] = search ?: ""
        model["filter"] = filter ?: "all"
        model["selected"] = selection
        model["page"] = mapOf("title" to PAGE_TITLE)
        model["tabs"] = mainPageTabsFrom(results, selection, ThaiFormatter.now())
        appointmentRepository.findByIdOrNull(selection)?.let { model["card"] = mainPageCardFrom(it) }
        return "html/index"
    }

    fun loc(ap: Appointment): Map<String, String>? = ap.location?.let { loc ->
        mapOf(
            "km" to String.format(Locale.ENGLISH, "%.2f", loc.distanceFromShop()),
            "prov" to loc.provinceMini(),
            "dist" to loc.districtMini(),
            "zone" to loc.calculateZone().toString()
        )
    }

    fun mainPageTabsFrom(results: List<Appointment>, selection: Long, today: ZonedDateTime) = results
        .sortedByDescending { ap -> ap.getTimeLastUpdated() }.map { ap ->
            mapOf(
                "id" to ap.id!!,
                "picked" to (ap.id!! == selection).toString(),
                "name" to ap.customer!!.displayName!!,
                "subject" to ap.serviceChoice!!.service!!.name!!,
                "choice" to ap.serviceChoice!!.name!!,
                "desc" to "${ap.serviceChoice!!.name!!}<br>" + (
                    ap.timeframe?.let { "${it.getCombinedDate(true)} ${it.getCombinedTime(true)}<br>" } ?: ""
                    ) + ap.getLocationDescriptor(true).replace("\\n", "<br>"),
                "src" to ap.customer!!.displayImage!!.toString(),
                "appType" to appointmentType(ap),
                "selected" to (ap.id!! == selection && selection != -1L),
                "color" to (appointmentColor[appointmentType(ap)] ?: "#ffffff"),
                "loc" to loc(ap),
                "updated" to ThaiFormatter.asZone(ap.getTimeLastUpdated()).let { day ->
                    ThaiFormatter.format(
                        day,
                        when {
                            day.toLocalDate() == today.toLocalDate() -> "H:mm"
                            day.toLocalDate() >= today.plusWeeks(1).toLocalDate() -> "EEEE"
                            else -> "d MMM"
                        }
                    )
                },
                "typeText" to ap.serviceChoice!!.getLocationText(),
                "time" to ap.timeframe?.let { tf ->
                    mapOf(
                        "date" to tf.getCombinedDate(mini = true),
                        "timeStart" to tf.getCombinedTime(mini = true).split(" - ")[0],
                        "timeEnd" to tf.getCombinedTime(mini = true).split(" - ")[1]
                    )
                },
                "serviceType" to ap.serviceChoice!!.serviceType!!.name
            )
        }

    fun schedulePageTabFrom(ap: Appointment): Map<String, Any?> = mapOf(
        "id" to ap.id!!,
        "name" to ap.customer!!.displayName!!,
        "service" to ap.serviceChoice!!.service!!.name!!,
        "choice" to ap.serviceChoice!!.name!!,
        "desc" to "${ap.serviceChoice!!.name!!}<br>" + (
            ap.timeframe?.let { "${it.getCombinedDate(true)} ${it.getCombinedTime(true)}<br>" } ?: ""
            ) + ap.getLocationDescriptor(true).replace("\\n", "<br>"),
        "src" to ap.customer!!.displayImage!!.toString(),
        "appType" to appointmentType(ap),
        "serviceType" to ap.serviceChoice!!.serviceType!!.name,
        "loc" to loc(ap),
        "typeText" to ap.serviceChoice!!.getLocationText(),
        "time" to ap.timeframe!!.let { tf ->
            mapOf(
                "padStart" to tf.paddedStartTime!!,
                "start" to tf.startTime!!,
                "end" to tf.endTime!!,
                "padEnd" to tf.paddedEndTime!!,
            ).mapValues { (_, v) -> ThaiFormatter.format(ThaiFormatter.asZone(v), "HH:mm") }
        },
        "hideStart" to ap.timeframe!!.let { it.paddedStartTime == it.startTime },
        "hideEnd" to ap.timeframe!!.let { it.endTime == it.paddedEndTime },
    )

    fun mainPageCardFrom(ap: Appointment): Map<String, Any?> = mapOf(
        "id" to ap.id!!,
        "name" to ap.customer!!.displayName!!,
        "fullName" to ap.customer!!.fullName!!,
        "group" to ap.serviceChoice!!.service!!.category!!.name!!,
        "service" to ap.serviceChoice!!.service!!.name!!,
        "choice" to ap.serviceChoice!!.name!!,
        "location" to ap.getLocationDescriptor().replace("\\n", " "),
        "date" to ap.timeframe!!.getCombinedDate(),
        "dateShort" to ap.timeframe!!.getCombinedDate(mini = true),
        "time" to ap.timeframe!!.getCombinedTime(),
        "timeStart" to ap.timeframe!!.getCombinedTime(mini = true).split(" - ")[0],
        "timeEnd" to ap.timeframe!!.getCombinedTime(mini = true).split(" - ")[1],
        "final" to (ap.approved != null),
        "type" to ap.serviceChoice!!.serviceType!!.name,
        "src" to ap.customer!!.displayImage!!.toString(),
        "slip" to (ap.slipImage ?: getImg("no-slip.png")).toString(),
        "lastUpdated" to ThaiFormatter.format(ThaiFormatter.asZone(ap.getTimeLastUpdated()), DATE_FORMAT),
        "slipFinal" to (
            (ap.slipFinal ?: ap.slipAdded)?.let {
                ThaiFormatter.format(ThaiFormatter.asZone(it), DATE_FORMAT)
            } ?: "ไม่มีข้อมูล"
            ),
        "timestamp" to mapOf(
            "selection_added" to ap.selectionAdded,
            "selection_final" to ap.selectionFinal,
            "slip_final" to ap.slipFinal,
            "approved" to ap.approved,
            "finished" to ap.finished
        ).mapValues { (_, v) ->
            v?.let { ThaiFormatter.format(ThaiFormatter.asZone(v), DATE_FORMAT) } ?: "ไม่มีข้อมูล"
        },
        "loc" to ap.location?.let { loc ->
            mapOf(
                "lat" to loc.latitude!!,
                "lon" to loc.longitude!!,
                "prov" to loc.province!!,
                "dist" to loc.districtMini(),
                "subd" to loc.subdistrictMini(),
                "km" to String.format(Locale.ENGLISH, "%.2f", loc.distanceFromShop()),

            )
        },
        "color" to appointmentColor(appointmentType(ap))
    )

    fun appointmentColor(type: String): String = when (type) {
        "finished" -> "#404040"
        "ready" -> "#40c365"
        "confirmation" -> "#ffc94c"
        "payment" -> "#41ccb4"
        "booking" -> "#298071"
        else -> "#ffffff"
    }

    fun appointmentType(appointment: Appointment) = appointment.run {
        listOf(
            finished to "finished",
            approved to "ready",
            slipFinal to "confirmation",
            selectionFinal to "payment",
            selectionAdded to "booking"
        )
    }.firstOrNull { it.first != null }?.second ?: "none"

    fun filterByOption(filter: String?): (Appointment) -> Boolean = when (filter) {
        "finished", "ready", "confirmation", "payment" -> {
            { appointmentType(it) == filter }
        }

        else -> {
            { true }
        }
    }

    @Suppress("FunctionOnlyReturningConstant", "UnusedParameter")
    @GetMapping("/login")
    fun login(model: ModelMap): String {
        return "html/login"
    }

    @GetMapping("/schedule")
    fun schedule(
        model: ModelMap,
        @RequestParam("day") day: Int?,
        @RequestParam("month") month: Int?,
        @RequestParam("year") year: Int?
    ): String {
        val today = ThaiFormatter.now().toLocalDate()

        val mn: Int = month ?: today.monthValue
        val yr: Int = year ?: today.year
        val dy: Int? = day ?: if (mn == month && yr == year) today.dayOfMonth else null

        mapOf(
            "page" to mapOf("title" to "แอปพลิเคชัน HoroAppoint"),
            "weekT" to listOf("จันทร์", "อังคาร", "พุธ", "พฤหัสบดี", "ศุกร์", "เสาร์", "อาทิตย์"),
            "dy" to (dy ?: ""),
            "mn" to mn,
            "yr" to yr,
            "days" to (1..YearMonth.of(yr, mn).lengthOfMonth()).map { mapOf("id" to it, "text" to it.toString()) },
            "months" to (1..MONTHS_OF_YEAR).map {
                mapOf("id" to it, "text" to ThaiFormatter.format(LocalDate.of(yr, it, 1), "MMMM"))
            },
            "years" to (yr downTo yr - MAX_YEARS_BACKLOG).map {
                mapOf("id" to it, "text" to ThaiFormatter.format(LocalDate.of(it, 1, 1), "yyyy"))
            },
            "rows" to (
                dy?.let { timeframeRepository.findTimeInDay(LocalDate.of(yr, mn, dy)).sortedBy { it.startTime } }
                    ?.map { schedulePageTabFrom(it.appointment!!) } ?: listOf()
                )
        ).let(model::addAllAttributes)
        monthLines(year, month).let(model::addAllAttributes)
        return "html/schedule"
    }

    fun monthLines(year: Int?, month: Int?): Map<String, List<Any>> {
        val (d, data) = timeframeRepository.findTimeInMonth(
            YearMonth.now().run { withYear(year ?: this.year) }.run { withMonth(month ?: this.monthValue) }
        )

        val dSt = d.indexOf(1)
        val dEn = d.asSequence().drop(dSt + 1).indexOf(1).let { if (it == -1) d.size - 1 else it + dSt + 1 }

        val list = List(MONTHLY_DISPLAY_DAYS) { mutableListOf<Timeframe>() }.apply {
            data.forEachIndexed { index, timeframes ->
                timeframes.sortedBy { it.startTime }.forEach { this[index].add(it) }
            }
        }

        val x: Map<String, List<Any>> = mapOf(
            "mDay" to d,
            "mStat" to List(MONTHLY_DISPLAY_DAYS) { i ->
                mapOf(
                    "same" to (i in dSt..dEn),
                    "blank" to list[i].isEmpty(),
                    "count" to "${list[i].size} งาน",
                    "time" to totalCoveredTime(list[i].map { it.paddedStartTime!! to it.paddedEndTime!! })
                        .let {
                            ThaiFormatter.format(LocalTime.ofSecondOfDay(0L) + it, "H ชม. m นท.")
                                .removePrefix("0 ชม. ").removeSuffix(" 0 นท.")
                        }
                )
            },
            "mLines" to list.map { li ->
                li.sortedBy { it.startTime }.map {
                    mapOf(
                        "appType" to appointmentType(it.appointment!!),
                        "serviceType" to it.appointment!!.serviceChoice!!.serviceType!!.name,
                        "start" to ThaiFormatter.format(ThaiFormatter.asZone(it.startTime!!), "HH:mm"),
                        "zone" to it.appointment!!.location?.calculateZone(),
                        "prov" to it.appointment!!.location?.province?.let { pr ->
                            if (pr == "กรุงเทพมหานคร") "กรุงเทพฯ" else pr.removePrefix("จังหวัด")
                        }
                    )
                }
            }
        )
        return x
    }

    fun totalCoveredTime(pairs: List<Pair<Instant, Instant>>): Duration {
        val sortedPairs = pairs.sortedWith(
            compareBy<Pair<Instant, Instant>> { it.first }.thenByDescending { it.second }
        )
        var cover = Duration.ZERO
        var end = Instant.MIN
        for ((x, y) in sortedPairs) {
            when {
                x.isAfter(end) -> cover += Duration.between(x, y)
                y.isAfter(end) -> cover += Duration.between(end, y)
            }
            end = maxOf(end, y)
        }
        return cover
    }
}
