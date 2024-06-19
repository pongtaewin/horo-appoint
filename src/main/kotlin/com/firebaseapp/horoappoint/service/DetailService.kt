package com.firebaseapp.horoappoint.service

import com.firebaseapp.horoappoint.ThaiFormatter
import com.firebaseapp.horoappoint.entity.Appointment
import com.firebaseapp.horoappoint.entity.Location
import com.firebaseapp.horoappoint.entity.ServiceType
import com.firebaseapp.horoappoint.repository.AppointmentRepository
import com.firebaseapp.horoappoint.repository.CustomerRepository
import com.firebaseapp.horoappoint.repository.LocationRepository
import com.firebaseapp.horoappoint.repository.ServiceChoiceRepository
import com.firebaseapp.horoappoint.repository.ServiceRepository
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.model.AddressComponent
import com.google.maps.model.AddressComponentType
import com.google.maps.model.LatLng
import com.linecorp.bot.messaging.model.LocationAction
import com.linecorp.bot.messaging.model.TextMessage
import com.linecorp.bot.webhook.model.Event
import com.linecorp.bot.webhook.model.MessageEvent
import com.linecorp.bot.webhook.model.PostbackEvent
import com.linecorp.bot.webhook.model.ReplyEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import java.text.DecimalFormat
import java.util.*
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

private const val STR_CHOOSE_SERVICE = "เลือกบริการที่ต้องการ"
private const val STR_CHANGE_SERVICE_GROUP = "เปลี่ยนกลุ่มบริการ"
private const val STR_CHANGE_SERVICE = "เปลี่ยนบริการ"

@Suppress("TooManyFunctions", "LongParameterList")
@Service
class DetailService(
    @Value("\${gcp-maps.api-key}")
    private val apiKey: String,
    private val messageService: MessageService,
    private val appointmentRepository: AppointmentRepository,
    private val customerRepository: CustomerRepository,
    private val locationRepository: LocationRepository,
    private val serviceChoiceRepository: ServiceChoiceRepository,
    private val serviceRepository: ServiceRepository,
    private val schedulingService: SchedulingService
) {

    val context: GeoApiContext = GeoApiContext.Builder().apiKey(apiKey).build()

    private val locIcon: String = "location.png"
    private val qrLocation = messageService.quickReplyOf(locIcon, LocationAction("เลือกตำแหน่ง"))
    private val qrLocationNew = messageService.quickReplyOf(locIcon, LocationAction("เลือกตำแหน่งใหม่"))
    private val qrServiceEdit = { appointment: Appointment ->
        messageService.quickReplyOf(
            "choice.png",
            "เปลี่ยนบริการ",
            "serviceChoice?id=${appointment.serviceChoice!!.service!!.category!!.id!!}"
        )
    }

    // require params ID
    fun handleServiceChoiceEvent(event: PostbackEvent, params: Map<String, String>) {
        val service = serviceRepository.findById((params["id"] ?: error("Null argument 'id'")).toLong()).get()
        val choices = serviceChoiceRepository.findByServiceAndVisibleTrueOrderByIdAsc(service)

        // if the choice requires location, get it and use that branch only
        if (choices[0].serviceType!! == ServiceType.MEETUP) {
            return handleLocationEvent(event, params)
        }

        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/service_choice.txt",
                ModelMap().apply {
                    this["service"] = mapOf(
                        "id" to service.id!!,
                        "category_id" to service.category!!.id!!,
                        "name" to service.name!!,
                        "desc" to service.description!!,
                        "price" to service.getMinPriceRounded(),
                        "image" to service.getDisplayImageOrDefault(),
                        "same_price" to (service.minPrice != service.maxPrice)
                    )
                    this["choices"] = choices.map { choice ->
                        mapOf(
                            "id" to choice.id!!,
                            "name" to choice.name!!,
                            "desc" to choice.description!!,
                            "price" to choice.getPriceRounded(),
                            "location" to choice.getLocationText(),
                            "duration" to choice.getDurationText()
                        )
                    }
                },
                STR_CHOOSE_SERVICE,
                messageService.quickReplyOf("category.png", STR_CHANGE_SERVICE_GROUP, "serviceCategory"),
                messageService.quickReplyOf(
                    "service.png",
                    STR_CHANGE_SERVICE,
                    "service?id=${service.category!!.id!!}"
                )
            )
        )
    }

    fun <T> handleLocationEvent(event: T, params: Map<String, String>) where T : Event, T : ReplyEvent {
        val id = params["id"]?.toLong()
            ?: appointmentRepository.findByEvent(event).getOrNull()
                ?.let { it.serviceChoice!!.service!!.id!! }
            ?: error("No Service ID parameter")

        var extraMessage: TextMessage? = null

        if (params["locErr"] == "not-found") {
            extraMessage = TextMessage(
                "ไม่พบสถานที่รับบริการที่เลือก\n" +
                    "กรุณาเลือกสถานที่ให้ถูกต้อง แล้วลองอีกครั้ง"
            )
        }

        if (params["locErr"] == "out-of-service-area") {
            extraMessage =
                TextMessage(
                    "อาจารย์ไม่สามารถให้บริการที่สถานที่นี้ได้\n" +
                        "(ตั้งอยู่นอกประเทศไทย) กรุณาเลือกสถานที่อื่น\n" +
                        "หรือติดต่ออาจารย์โดยตรงทางห้องแชทนี้ครับ"
                )
        }

        if (params["overwrite"] == "location") {
            val appointment = appointmentRepository.findByEvent(event).getOrNull()
                ?: error("Overwrite location with no appointment created")
            appointmentRepository.save(appointment.apply { location = null })
        }

        customerRepository.save(
            customerRepository.findByEvent(event).get().apply { state = "location?id=$id" }
        )

        messageService.replyMessage(
            event,
            extraMessage,
            messageService.processTemplateAndMakeMessage(
                "json/location.txt",
                ModelMap(),
                "กรุณาเลือกตำแหน่งรับบริการ",
                qrLocation
            )
        )
    }

    fun handleLocationReceivedEvent(event: MessageEvent, params: Map<String, String>) {
        val customer = customerRepository.findByEvent(event).get()
        val serviceId: Long = customer.state?.let { st ->
            if (st.startsWith("location?id=")) st.removePrefix("location?id=").toLongOrNull() else null
        } ?: error("Erroneous State")
        val service = serviceRepository.findById(serviceId).getOrNull() ?: error("No service with specified id")

        val locationRes = mapCustomerLocation(
            (params["lat"] ?: error("Null attribute 'lat')")).toDouble(),
            (params["lon"] ?: error("Null attribute 'lon')")).toDouble(),
            context
        )

        when {
            locationRes == null ->
                return handleLocationEvent(event, (params.toMutableMap().apply { put("locErr", "not-found") }))

            locationRes.isEmpty ->
                return handleLocationEvent(
                    event,
                    (params.toMutableMap().apply { put("locErr", "out-of-service-area") })
                )
        }

        val location = locationRes!!.get()

        val zone = location.calculateZone()
        appointmentRepository.save(
            appointmentRepository.findByEvent(event).getOrElse {
                Appointment().apply {
                    this.customer = customer
                    selectionAdded = ThaiFormatter.now().toInstant()
                }
            }.apply {
                this.location = locationRepository.save(location)
                this.serviceChoice = serviceChoiceRepository
                    .findByServiceAndVisibleTrueOrderByIdAsc(service).find { it.zone == zone }
                    ?: error("No ServiceChoice which matches Service $serviceId and Zone $zone")
            }
        )
        handleLocationAddedEvent(event, params)
    }

    @Suppress("MagicNumber", "UnusedParameter")
    fun handleLocationAddedEvent(event: MessageEvent, params: Map<String, String>) {
        val appointment = appointmentRepository.findByEvent(event).get()
        val location = appointment.location!!
        messageService.replyMessage(
            event,
            messageService.processTemplateAndMakeMessage(
                "json/location_added.txt",
                ModelMap().apply {
                    put("province", location.province!!)
                    put("district", location.district!!)
                    put("subdistrict", location.subdistrict!!)
                    put("km", DecimalFormat("#,##0.00").format(location.distanceFromShop()))
                    put(
                        "zone",
                        when (val zone = location.calculateZone()) {
                            1 -> "โซนที่ 1 (ไม่เกิน 50 กม.)"
                            2 -> "โซนที่ 2 (50-150 กม.)"
                            3 -> "โซนที่ 3 (เกิน 150 กม.)"
                            else -> error("Invalid zone $zone")
                        }
                    )
                    put("duration", appointment.serviceChoice!!.getDurationText())
                    put("price", appointment.serviceChoice!!.getPriceRounded())
                },
                "กรุณาตรวจสอบตำแหน่งรับบริการ",
                qrServiceEdit(appointment),
                qrLocationNew
            )
        )
    }

    fun handleSelectEvent(event: PostbackEvent, params: Map<String, String>) {
        val choice = serviceChoiceRepository.findById(
            (params["choice"] ?: error("Null attribute 'choice'")).toLong()
        ).get()
        val appointment =
            appointmentRepository.findByEvent(event).getOrNull()?.apply {
                serviceChoice = choice
            } ?: Appointment().apply {
                customer = customerRepository.findByEvent(event).get()
                serviceChoice = choice
                selectionAdded = ThaiFormatter.now().toInstant()
            }
        appointmentRepository.save(appointment)
        schedulingService.handleDateEvent(event, params)
    }

    private fun mapCustomerLocation(lat: Double, lon: Double, context: GeoApiContext): Optional<Location>? =
        GeocodingApi.reverseGeocode(context, LatLng(lat, lon)).language("th")
            .await().map { it.addressComponents.toList() }
            .firstNotNullOfOrNull(::getInformationIfComplete).let { result ->
                when {
                    result == null -> null
                    result.isEmpty -> Optional.empty()
                    else -> result.get().let { (prov, dist, subd) ->
                        Optional.of(
                            Location().apply {
                                latitude = lat
                                longitude = lon
                                province = prov
                                district = dist
                                subdistrict = subd
                            }
                        )
                    }
                }
            }

    private fun getInformationIfComplete(result: List<AddressComponent>): Optional<List<String>>? =
        (
            result.also {
                if (result.find { AddressComponentType.COUNTRY in it.types }?.shortName != "TH") {
                    return Optional.empty()
                }
            }.find { AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1 in it.types }
            )?.let { prov ->
            if ("กรุงเทพมหานคร" in prov.longName) {
                val dist = result.find { AddressComponentType.SUBLOCALITY_LEVEL_1 in it.types } ?: return null
                val subd = result.find { AddressComponentType.SUBLOCALITY_LEVEL_2 in it.types } ?: return null
                listOf(
                    "กรุงเทพมหานคร",
                    "เขด" + dist.longName.replace("เขด", "").trim(),
                    "แขวง" + subd.longName.replace("แขวง", "").trim()
                )
            } else {
                val distA =
                    result.find { AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_2 in it.types } ?: return null
                val subdA = result.find { AddressComponentType.LOCALITY in it.types } ?: return null
                listOf(
                    "จังหวัด" + prov.longName.replace("จังหวัด", "").trim(),
                    "อำเภอ" + distA.longName.replace("อำเภอ", "").trim(),
                    "ตำบล" + subdA.longName.replace("ตำบล", "").trim()
                )
            }
        }?.let { Optional.of(it) }
}
