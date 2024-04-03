package com.firebaseapp.horoappoint.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.firebaseapp.horoappoint.model.CustomerLocation
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class LocationService (
    @Value("\${location-iq.api-key}") val apiKey: String
){
    fun retrieveLocationFromAPI(lat: String, lon: String): JsonNode? = RestClient
        .create()
        .post()
        .uri("https://eu1.locationiq.com/v1/reverse?lat=$lat&lon=$lon&format=json&accept-language=th&key=${apiKey}")
        .contentType(MediaType.APPLICATION_JSON)
        .retrieve()
        .body<String>()
        .let { result -> ObjectMapper().readTree(result) }


    fun mapCustomerLocation(lat:String, lon: String){
        val node = retrieveLocationFromAPI(lat, lon)!!
        CustomerLocation().apply{
            latitude = lat.toDouble()
            longitude = lon.toDouble()
            province = node["address"]["state"].textValue()
            district = node["address"]["district"].textValue()
            subdistrict = node["address"]["municipality"].textValue()
        }
    }

}