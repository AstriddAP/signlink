package com.signlink.data.remote

import com.google.gson.annotations.SerializedName

data class AnalysisResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("result")
    val result: String?,

    @SerializedName("predictions")
    val predictions: List<Prediction>?,

    @SerializedName("processing_time")
    val processingTime: Double?
)

data class Prediction(
    @SerializedName("label")
    val label: String?,

    @SerializedName("confidence")
    val confidence: Double?,

    @SerializedName("bounding_box")
    val boundingBox: BoundingBox?
)

data class BoundingBox(
    @SerializedName("xmin")
    val xmin: Int?,

    @SerializedName("ymin")
    val ymin: Int?,

    @SerializedName("xmax")
    val xmax: Int?,

    @SerializedName("ymax")
    val ymax: Int?
)
