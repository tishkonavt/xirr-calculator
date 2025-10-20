package com.xirr

import com.xirr.calculator.ReturnChartService
import com.xirr.chart.ChartGenerator
import com.xirr.data.CsvDataProvider
import java.io.File

fun main(args: Array<String>) {
    println("=".repeat(60))
    println("   📊 XIRR Calculator — CSV mode")
    println("=".repeat(60))
    println()

    val csvPath = args.firstOrNull()
        ?: System.getenv("XIRR_CSV_PATH")
        ?: "portfolio_data.csv"

    println("Использую CSV: $csvPath")

    val snapshots = try {
        val provider = CsvDataProvider(csvPath)
        provider.getPortfolioSnapshots()
    } catch (e: Exception) {
        println("❌ Ошибка чтения CSV: ${e.message}")
        return
    }

    if (snapshots.isEmpty()) {
        println("❌ Нет данных в CSV: $csvPath")
        return
    }

    // Статистика
    println("\n" + "=".repeat(60))
    println("   📈 Статистика портфеля")
    println("=".repeat(60))
    println("Период: ${snapshots.first().date} - ${snapshots.last().date}")
    println("Всего дней: ${snapshots.size}")

    val totalInvested = snapshots.sumOf { it.cashIn } - snapshots.sumOf { it.cashOut }
    val finalValue = snapshots.last().valuation

    println("Всего вложено: ${formatNumber(totalInvested)}")
    println("Финальная оценка: ${formatNumber(finalValue)}")
    println("Абсолютная прибыль: ${formatNumber(finalValue - totalInvested)}")
    println()

    // Расчёт XIRR
    println("⏳ Расчёт доходности...")
    val chartService = ReturnChartService()
    val startTime = System.currentTimeMillis()
    val xirrPoints = chartService.buildReturnChart(snapshots)
    val cumulativePoints = chartService.buildCumulativeReturnChart(snapshots)
    val calculationTime = (System.currentTimeMillis() - startTime) / 1000.0
    println("✅ Расчёт завершен за ${String.format("%.2f", calculationTime)} сек")
    println()

    // Результаты
    println("=".repeat(60))
    println("   🎯 Итоговые результаты")
    println("=".repeat(60))

    val finalXirr = xirrPoints.lastOrNull()
    val finalCumulative = cumulativePoints.lastOrNull()

    if (finalXirr != null) {
        println("Годовая доходность (XIRR): ${String.format("%.2f%%", finalXirr.xirrRate * 100)}")
    }
    if (finalCumulative != null) {
        println("Кумулятивная доходность: ${String.format("%.2f%%", finalCumulative.xirrRate * 100)}")
    }
    println()

    // Генерация двух PNG
    println("⏳ Генерация графиков...")
    val chartGenerator = ChartGenerator()
    try {
        val xirrFile = chartGenerator.generateChart(xirrPoints, "xirr_annual.png", isCumulative = false)
        println("✅ График годовой доходности: ${File(xirrFile.absolutePath).absolutePath}")

        val cumulativeFile = chartGenerator.generateChart(cumulativePoints, "cumulative_return.png", isCumulative = true)
        println("✅ График кумулятивной доходности: ${File(cumulativeFile.absolutePath).absolutePath}")
    } catch (e: Exception) {
        println("❌ Ошибка генерации графиков: ${e.message}")
    }

    // Создать CSV с двумя новыми столбцами (читаем исходный CSV из FS или resources)
    try {
        val origFile = File(csvPath)
        val lines: List<String> = if (origFile.exists()) {
            origFile.readLines(Charsets.UTF_8)
        } else {
            // Попытка загрузить из classpath (resources)
            val resStream = object {}.javaClass.classLoader.getResourceAsStream(csvPath)
            if (resStream != null) {
                resStream.bufferedReader(Charsets.UTF_8).use { it.readLines() }
            } else {
                println("⚠️ Оригинальный CSV не найден в файловой системе и не найден в resources: $csvPath")
                // не хватает исходного текста CSV для добавления колонок — создаём минимальный CSV из snapshots
                val header = "date,valuation,cashIn,cashOut"
                val generated = mutableListOf<String>()
                generated.add(header)
                snapshots.forEach {
                    generated.add("${it.date},${it.valuation},${it.cashIn},${it.cashOut}")
                }
                generated
            }
        }

        if (lines.isEmpty()) {
            println("❌ Пустой исходный CSV, ничего не записано.")
        } else {
            val outName = (File(csvPath).nameWithoutExtension) + "_with_xirr.csv"
            val outFile = File(outName)
            val header = lines.first()
            val newHeader = header + ",xirr_annual,xirr_cumulative"
            val outLines = mutableListOf<String>()
            outLines.add(newHeader)

            // Подготовка карты даты -> индекс снапшота
            val dateToIdx = snapshots.mapIndexed { idx, s -> s.date.toString() to idx }.toMap()
            val headerCols = header.split(",")
            val dateColIndex = headerCols.indexOfFirst { it.lowercase().contains("date") }

            if (lines.size - 1 == snapshots.size) {
                for (i in 1 until lines.size) {
                    val snapIdx = i - 1
                    val xirrVal = xirrPoints.getOrNull(snapIdx)?.xirrRate
                    val cumVal = cumulativePoints.getOrNull(snapIdx)?.xirrRate
                    val xirrStr = xirrVal?.let { String.format("%.8f", it) } ?: ""
                    val cumStr = cumVal?.let { String.format("%.8f", it) } ?: ""
                    outLines.add(lines[i] + "," + xirrStr + "," + cumStr)
                }
            } else if (dateColIndex >= 0) {
                for (i in 1 until lines.size) {
                    val cols = lines[i].split(",")
                    val dateVal = cols.getOrNull(dateColIndex)?.trim()?.trim('"')
                    val snapIdx = dateVal?.let { dateToIdx[it] }
                    val xirrVal = snapIdx?.let { xirrPoints.getOrNull(it)?.xirrRate }
                    val cumVal = snapIdx?.let { cumulativePoints.getOrNull(it)?.xirrRate }
                    val xirrStr = xirrVal?.let { String.format("%.8f", it) } ?: ""
                    val cumStr = cumVal?.let { String.format("%.8f", it) } ?: ""
                    outLines.add(lines[i] + "," + xirrStr + "," + cumStr)
                }
            } else {
                println("⚠️ Несоответствие строк CSV и количества снимков. Попытка дописать по минимальному количеству.")
                val min = minOf(lines.size - 1, snapshots.size)
                for (i in 1..min) {
                    val snapIdx = i - 1
                    val xirrVal = xirrPoints.getOrNull(snapIdx)?.xirrRate
                    val cumVal = cumulativePoints.getOrNull(snapIdx)?.xirrRate
                    val xirrStr = xirrVal?.let { String.format("%.8f", it) } ?: ""
                    val cumStr = cumVal?.let { String.format("%.8f", it) } ?: ""
                    outLines.add(lines[i] + "," + xirrStr + "," + cumStr)
                }
                if (lines.size - 1 > min) {
                    for (i in (min + 1) until lines.size) {
                        outLines.add(lines[i] + ",,")
                    }
                }
            }

            outFile.writeText(outLines.joinToString("\n"), Charsets.UTF_8)
            println("✅ Создан CSV с XIRR: ${outFile.absolutePath}")
        }
    } catch (e: Exception) {
        println("❌ Ошибка при создании CSV с XIRR: ${e.message}")
    }

    println()
    println("=".repeat(60))
    println("   ✨ Готово!")
    println("=".repeat(60))
}

private fun formatNumber(value: Double): String {
    return String.format("%,.0f", value)
}