package com.segmentanalyzer.data.local.converter

import androidx.room.TypeConverter
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType

class RideTypeConverters {

    @TypeConverter
    fun fromActivityType(value: ActivityType): String = value.name

    @TypeConverter
    fun toActivityType(value: String): ActivityType = ActivityType.valueOf(value)

    @TypeConverter
    fun fromActivitySource(value: ActivitySource): String = value.name

    @TypeConverter
    fun toActivitySource(value: String): ActivitySource = ActivitySource.valueOf(value)

    @TypeConverter
    fun fromFloatList(value: List<Float>): String = value.joinToString(",")

    @TypeConverter
    fun toFloatList(value: String): List<Float> =
        if (value.isEmpty()) emptyList() else value.split(",").map { it.toFloat() }
}
