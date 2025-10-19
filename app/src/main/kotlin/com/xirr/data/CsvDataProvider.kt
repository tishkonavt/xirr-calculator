package com.xirr.data

import com.xirr.models.PortfolioSnapshot
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Провайдер данных из CSV файла
 */
class CsvDataProvider(private val resourceName: String = "portfolio_data.csv") : DataProvider {
    
    override fun getPortfolioSnapshots(): List<PortfolioSnapshot> {
        println("Чтение файла: $resourceName из resources")
        
        // Читаем файл из resources через classloader
        val inputStream = this::class.java.classLoader.getResourceAsStream(resourceName)
            ?: throw IllegalArgumentException("Файл не найден в resources: $resourceName")
        
        val lines = BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.readLines()
        }
        
        if (lines.isEmpty()) {
            throw IllegalArgumentException("Файл пуст")
        }
        
        // Пропускаем заголовок
        val dataLines = lines.drop(1)
        
        val snapshots = dataLines.mapNotNull { line ->
            parseLine(line)
        }
        
        println("Загружено ${snapshots.size} записей из CSV")
        
        return snapshots
    }
    
    /**
     * Парсит одну строку CSV
     * Формат: date;valuation;cashIn;cashOut
     * Числа в европейском формате: "400 000,00"
     */
    private fun parseLine(line: String): PortfolioSnapshot? {
        if (line.isBlank()) return null
        
        val parts = line.split(";")
        
        if (parts.size < 4) {
            println("Предупреждение: некорректная строка: $line")
            return null
        }
        
        try {
            // Парсим дату (формат: yyyy-MM-dd)
            val date = LocalDate.parse(parts[0].trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            
            // Парсим числа (убираем пробелы, заменяем запятую на точку)
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
    
    /**
     * Парсит число в европейском формате
     * Пример: "400 000,00" → 400000.00
     */
    private fun parseEuropeanNumber(value: String): Double {
        return value.trim()
            .replace(" ", "")      // Убираем пробелы
            .replace(",", ".")     // Заменяем запятую на точку
            .toDouble()
    }
}