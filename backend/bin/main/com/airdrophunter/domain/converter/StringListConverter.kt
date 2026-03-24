package com.airdrophunter.domain.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * JPA converter that serialises List<String> as a pipe-delimited TEXT column.
 * Using pipes instead of commas avoids conflicts with step descriptions that
 * might contain commas.
 */
@Converter
class StringListConverter : AttributeConverter<List<String>, String> {

    override fun convertToDatabaseColumn(attribute: List<String>?): String =
        attribute?.joinToString(separator = "|") ?: ""

    override fun convertToEntityAttribute(dbData: String?): List<String> =
        if (dbData.isNullOrBlank()) emptyList()
        else dbData.split("|").map { it.trim() }.filter { it.isNotEmpty() }
}
