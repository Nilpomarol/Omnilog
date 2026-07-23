package com.nilpo.contenttracker.core.repository

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

internal open class ProviderImportFileException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

internal class ProviderImportEncodingException(message: String) : ProviderImportFileException(message)

internal class MalformedProviderCsvException(
    message: String,
    val rowNumber: Int? = null,
) : ProviderImportFileException(message)

internal enum class ProviderCsvValidationIssue {
    EmptyFile,
    MissingRequiredColumns,
    NoDataRows,
    NoUsableRows,
}

internal class ProviderCsvValidationException(
    val issue: ProviderCsvValidationIssue,
    val missingColumns: List<String> = emptyList(),
) : ProviderImportFileException(
    when (issue) {
        ProviderCsvValidationIssue.EmptyFile -> "Provider CSV file is empty"
        ProviderCsvValidationIssue.MissingRequiredColumns ->
            "Provider CSV is missing required columns: ${missingColumns.joinToString()}"
        ProviderCsvValidationIssue.NoDataRows -> "Provider CSV contains only a header"
        ProviderCsvValidationIssue.NoUsableRows -> "Provider CSV contains no usable rows"
    },
)

internal class MalformedProviderXmlException(
    message: String,
    cause: Throwable,
) : ProviderImportFileException(message, cause)

internal class ProviderImportFileTooLargeException : ProviderImportFileException(
    "Import file exceeds the ${MaxProviderImportBytes / (1024 * 1024)} MB limit",
)

internal fun InputStream.readProviderImportText(): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        if (total > MaxProviderImportBytes) throw ProviderImportFileTooLargeException()
        output.write(buffer, 0, count)
    }
    return decodeProviderImportText(output.toByteArray())
}

/** Decodes provider exports without silently replacing damaged bytes. */
internal fun decodeProviderImportText(bytes: ByteArray): String {
    val decoded = when {
        bytes.startsWith(0xEF, 0xBB, 0xBF) -> bytes.decodeStrict(StandardCharsets.UTF_8, offset = 3)
        bytes.startsWith(0xFF, 0xFE) -> bytes.decodeStrict(StandardCharsets.UTF_16LE, offset = 2)
        bytes.startsWith(0xFE, 0xFF) -> bytes.decodeStrict(StandardCharsets.UTF_16BE, offset = 2)
        bytes.looksLikeUtf16LittleEndian() -> bytes.decodeStrict(StandardCharsets.UTF_16LE)
        bytes.looksLikeUtf16BigEndian() -> bytes.decodeStrict(StandardCharsets.UTF_16BE)
        else -> runCatching { bytes.decodeStrict(StandardCharsets.UTF_8) }
            .getOrElse { bytes.decodeStrict(Charset.forName("windows-1252")) }
    }.removePrefix("\uFEFF")

    if (decoded.any { character -> character.isUnsupportedImportControl() }) {
        throw ProviderImportEncodingException("Import file contains binary or unsupported control characters")
    }
    return decoded
}

/** RFC-style CSV reader shared by IMDb and StoryGraph, with structural damage detection. */
internal fun parseProviderCsvTable(csv: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val cell = StringBuilder()
    var index = 0
    var inQuotes = false
    var quoteClosed = false

    fun finishCell() {
        row += cell.toString()
        cell.clear()
        quoteClosed = false
    }

    fun finishRow() {
        finishCell()
        if (row.any(String::isNotBlank)) rows += row.toList()
        row.clear()
    }

    while (index < csv.length) {
        val character = csv[index]
        when {
            inQuotes && character == '"' && csv.getOrNull(index + 1) == '"' -> {
                cell.append('"')
                index++
            }
            inQuotes && character == '"' -> {
                inQuotes = false
                quoteClosed = true
            }
            inQuotes -> cell.append(character)
            quoteClosed && character == ',' -> finishCell()
            quoteClosed && (character == '\n' || character == '\r') -> {
                if (character == '\r' && csv.getOrNull(index + 1) == '\n') index++
                finishRow()
            }
            quoteClosed && character.isWhitespace() -> Unit
            quoteClosed -> throw MalformedProviderCsvException(
                "Unexpected character after a quoted field on row ${rows.size + 1}",
                rowNumber = rows.size + 1,
            )
            character == '"' && cell.isEmpty() -> inQuotes = true
            character == '"' -> throw MalformedProviderCsvException(
                "Unexpected quote in an unquoted field on row ${rows.size + 1}",
                rowNumber = rows.size + 1,
            )
            character == ',' -> finishCell()
            character == '\n' || character == '\r' -> {
                if (character == '\r' && csv.getOrNull(index + 1) == '\n') index++
                finishRow()
            }
            else -> cell.append(character)
        }
        index++
    }

    if (inQuotes) {
        throw MalformedProviderCsvException(
            "Unclosed quoted field on row ${rows.size + 1}",
            rowNumber = rows.size + 1,
        )
    }
    if (cell.isNotEmpty() || row.isNotEmpty() || quoteClosed) finishRow()

    val columnCount = rows.firstOrNull()?.size ?: return emptyList()
    rows.drop(1).forEachIndexed { rowIndex, values ->
        if (values.size != columnCount) {
            throw MalformedProviderCsvException(
                "Row ${rowIndex + 2} has ${values.size} columns; expected $columnCount",
                rowNumber = rowIndex + 2,
            )
        }
    }
    return rows
}

private fun ByteArray.decodeStrict(
    charset: java.nio.charset.Charset,
    offset: Int = 0,
): String = try {
    charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(this, offset, size - offset))
        .toString()
} catch (error: CharacterCodingException) {
    throw ProviderImportEncodingException("Import file uses an unsupported or damaged text encoding")
}

private fun ByteArray.startsWith(vararg prefix: Int): Boolean =
    size >= prefix.size && prefix.indices.all { index -> this[index].toInt() and 0xFF == prefix[index] }

private fun ByteArray.looksLikeUtf16LittleEndian(): Boolean =
    size >= MinBomlessUtf16Bytes && utf16ZeroRatios().let { (even, odd) ->
        odd >= Utf16ZeroRatio && even < Utf16OppositeZeroRatio
    }

private fun ByteArray.looksLikeUtf16BigEndian(): Boolean =
    size >= MinBomlessUtf16Bytes && utf16ZeroRatios().let { (even, odd) ->
        even >= Utf16ZeroRatio && odd < Utf16OppositeZeroRatio
    }

private fun ByteArray.utf16ZeroRatios(): Pair<Double, Double> {
    val sampledSize = minOf(size, Utf16SampleBytes).let { it - it % 2 }
    if (sampledSize < 4) return 0.0 to 0.0
    val pairCount = sampledSize / 2
    val evenZeros = (0 until sampledSize step 2).count { this[it] == 0.toByte() }
    val oddZeros = (1 until sampledSize step 2).count { this[it] == 0.toByte() }
    return evenZeros.toDouble() / pairCount to oddZeros.toDouble() / pairCount
}

private fun Char.isUnsupportedImportControl(): Boolean =
    this == '\u0000' || this == '\uFFFD' || (code in 0x01..0x1F && this != '\n' && this != '\r' && this != '\t')

private const val MaxProviderImportBytes = 25 * 1024 * 1024
private const val Utf16SampleBytes = 512
private const val MinBomlessUtf16Bytes = 8
private const val Utf16ZeroRatio = 0.30
private const val Utf16OppositeZeroRatio = 0.05
