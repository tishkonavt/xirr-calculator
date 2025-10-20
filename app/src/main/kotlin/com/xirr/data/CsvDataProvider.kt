package com.xirr.data

import com.xirr.models.PortfolioSnapshot
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CsvDataProvider(private val filePath: String) : DataProvider {
    
    override fun getPortfolioSnapshots(): List<PortfolioSnapshot> {
        println("Чтение файла: $filePath")
        
        val lines = try {
            val file = File(filePath)
            if (file.exists()) {
                file.readLines()
            } else {
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
        
        if (snapshots.isEmpty()) {
            throw IllegalArgumentException("Нет корректных данных в CSV")
        }
        
        // Информация о первом дне
        val firstSnapshot = snapshots.first()
        if (firstSnapshot.cashIn == 0.0 && firstSnapshot.valuation > 0.0) {
            println("ℹ️  Первый день без ввода средств.")
            println("   Начальная оценка: ${String.format("%,.0f", firstSnapshot.valuation)}")
            println("   Будет использована как начальный ввод для расчета XIRR.")
            println()
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