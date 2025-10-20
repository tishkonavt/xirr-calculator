package com.xirr.data

import com.xirr.models.PortfolioSnapshot
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Провайдер данных из CSV файла
 */
class CsvDataProvider(private val filePath: String) : DataProvider {
    
    override fun getPortfolioSnapshots(): List<PortfolioSnapshot> {
        println("Чтение файла: $filePath")
        
        val lines = try {
            // Сначала пробуем как обычный файл
            val file = File(filePath)
            if (file.exists()) {
                file.readLines()
            } else {
                // Пробуем как ресурс
                val inputStream = this::class.java.classLoader.getResourceAsStream(filePath.removePrefix("app/src/main/resources/"))
                    ?: throw IllegalArgumentException("Файл не найден: $filePath")
                
                BufferedReader(InputStreamReader(inputStream)).use { it.readLines() }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Ошибка чтения файла: ${e.message}")
        }
        
        if (lines.isEmpty()) {
            throw IllegalArgumentException("Файл пуст")
        }
        
        val dataLines = lines.drop(1) // Пропускаем заголовок
        
        val snapshots = dataLines.mapNotNull { line ->
            parseLine(line)
        }
        
        println("Загружено ${snapshots.size} записей из CSV")
        
        return snapshots
    }
    
    private fun parseLine(line: String): PortfolioSnapshot? {
        if (line.isBlank()) return null
        
        val parts = line.split(";")
        
        if (parts.size < 4) {
            println("Предупреждение: некорректная строка: $line")
            return null
        }
        
        try {
            val date = LocalDate.parse(parts[0].trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            val valuation = parseEuropeanNumber(parts[1])
            val cashIn = parseEuropeanNumber(parts[2])
            val cashOut = parseEuropeanNumber(parts[3])
            
            return PortfolioSnapshot(
                date = date,
                valuation = valuation,
                cashIn = cashIn,
                cashOut = cashOut
            )
        } catch (e: Exception) {
            println("Ошибка парсинга строки: $line - ${e.message}")
            return null
        }
    }
    
    private fun parseEuropeanNumber(value: String): Double {
        if (value.trim().isEmpty()) return 0.0
        return value.trim()
            .replace(" ", "")
            .replace(",", ".")
            .toDouble()
    }
}