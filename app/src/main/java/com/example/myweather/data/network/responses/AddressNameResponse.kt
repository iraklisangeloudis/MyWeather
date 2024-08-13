package com.example.myweather.data.network.responses

import com.google.gson.annotations.SerializedName

data class AddressNameResponse(
    @SerializedName("address") val address: Address
)

data class Address(
    @SerializedName("hamlet") val hamlet: String? = null,
    @SerializedName("village") val village: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("town") val town: String? = null,
    @SerializedName("county") val county: String?= null,
    @SerializedName("suburb") val suburb: String?= null,
    @SerializedName("region") val region: String?= null,
    @SerializedName("state_district") val stateDistrict: String?= null,
    @SerializedName("country") val country: String?= null,
    @SerializedName("country_code") val countryCode: String? = null
)
