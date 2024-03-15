package com.firebaseapp.horoappoint

import com.firebaseapp.horoappoint.model.Timeframe
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import java.time.Instant


fun main(args: Array<String>) {
    runApplication<HoroAppointApplication>(*args)
}


@SpringBootApplication
@LineMessageHandler
class HoroAppointApplication() {

    /*
    @EventListener(ApplicationReadyEvent::class)
    fun initializeFirebase() {

    }

     */

    @EventListener(ApplicationReadyEvent::class)
    fun test() {
        val timeFrameList = listOf(
            Instant.parse("2007-12-03T10:15:30.00Z") to Instant.parse("2007-12-04T10:15:30.00Z"),
            Instant.parse("2007-12-10T10:15:30.00Z") to Instant.parse("2007-12-11T00:00:00.00Z"),
            Instant.parse("2022-12-03T10:15:30.00Z") to Instant.parse("2024-12-03T10:15:30.00Z"),
            Instant.parse("2023-12-03T10:15:30.00Z") to Instant.parse("2023-12-03T10:20:30.00Z"),
            Instant.parse("2023-12-03T10:15:30.00Z") to Instant.parse("2024-01-03T10:15:30.00Z"),
            Instant.parse("2023-11-03T10:15:30.00Z") to Instant.parse("2023-12-04T00:15:30.00Z"),
        ).map { (st, en) -> Timeframe().apply { startTime = st; endTime = en } }
        for (tf: Timeframe in timeFrameList) {
            log.info("Start Time equals ${tf.startTime} / ${tf.getStart()}")
            log.info("End Time equals ${tf.endTime} / ${tf.getEnd()}")
            log.info("Combined Date: ${tf.getCombinedDate()}")
            log.info("Combined Time: ${tf.getCombinedTime()}")
        }

    }

    // logger
    private val log = LoggerFactory.getLogger(HoroAppointApplication::class.java)


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


