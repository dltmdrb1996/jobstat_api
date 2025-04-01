package com.example.jobstat.core.base.mongo.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringListConverter : AttributeConverter<List<String>, String> {
    companion object {
        private const val SPLIT_CHAR = ","
    }

    override fun convertToDatabaseColumn(stringList: List<String>?): String = stringList?.joinToString(SPLIT_CHAR) ?: ""

    override fun convertToEntityAttribute(string: String?): List<String> = string?.split(SPLIT_CHAR) ?: emptyList()
}
