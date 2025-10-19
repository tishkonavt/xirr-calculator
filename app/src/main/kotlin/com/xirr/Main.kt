package com.xirr

import com.xirr.calculator.ReturnChartService
import com.xirr.chart.ChartGenerator
import com.xirr.data.CsvDataProvider

fun main() {
    println("=== XIRR Calculator - Реальные данные портфеля ===\n")
    
    println("Загрузка данных из CSV...")
    val dataProvider = CsvDataProvider("portfolio_data.csv")
    val snapshots = dataProvider.getPortfolioSnapshots()
    println()
    
    println("=== Статистика портфеля ===")
    println("Период: ${snapshots.first().date} - ${snapshots.last().date}")
    println("Всего дней: ${snapshots.size}")
    
    val totalInvested = snapshots.sumOf { it.cashIn } - snapshots.sumOf { it.cashOut }
    val finalValue = snapshots.last().valuation
    
    println("Всего вложено: ${String.format("%,.0f", totalInvested)}")
    println("Финальная оценка: ${String.format("%,.0f", finalValue)}")
    println("Абсолютная прибыль: ${String.format("%,.0f", finalValue - totalInvested)}")
    println("Кумулятивная доходность: ${String.format("%.2f%%", (finalValue / totalInvested - 1) * 100)}")
    println()
    
    val chartService = ReturnChartService()
    
    // ========== ГРАФИК 1: XIRR (годовая доходность) ==========
    println("=== Расчет графика XIRR (годовая доходность) ===")
    val startTime1 = System.currentTimeMillis()
    val xirrPoints = chartService.buildReturnChart(snapshots)
    val calculationTime1 = (System.currentTimeMillis() - startTime1) / 1000.0
    println("✓ Расчет завершен за ${String.format("%.2f", calculationTime1)} секунд")
    
    val finalXirr = xirrPoints.lastOrNull()
    if (finalXirr != null) {
        println("🎯 Итоговая годовая доходность (XIRR): ${String.format("%.2f%%", finalXirr.xirrRate * 100)}")
    }
    println()
    
    // ========== ГРАФИК 2: Кумулятивная доходность ==========
    println("=== Расчет кумулятивной доходности (за период) ===")
    val startTime2 = System.currentTimeMillis()
    val cumulativePoints = chartService.buildCumulativeReturnChart(snapshots)
    val calculationTime2 = (System.currentTimeMillis() - startTime2) / 1000.0
    println("✓ Расчет завершен за ${String.format("%.2f", calculationTime2)} секунд")
    
    val finalCumulative = cumulativePoints.lastOrNull()
    if (finalCumulative != null) {
        println("🎯 Итоговая доходность за период: ${String.format("%.2f%%", finalCumulative.xirrRate * 100)}")
    }
    println()
    
    // ========== Генерация графиков ==========
    println("=== Генерация графиков ===")
    val chartGenerator = ChartGenerator()
    
    try {
        // График 1: XIRR
        val xirrChart = chartGenerator.generateChart(
            xirrPoints, 
            "portfolio_xirr_annual.png", 
            isCumulative = false
        )
        println("✓ График XIRR (годовая): ${xirrChart.absolutePath}")
        
        // График 2: Кумулятивная
        val cumulativeChart = chartGenerator.generateChart(
            cumulativePoints, 
            "portfolio_cumulative_return.png", 
            isCumulative = true
        )
        println("✓ График кумулятивной доходности: ${cumulativeChart.absolutePath}")
        
    } catch (e: Exception) {
        println("✗ Ошибка при генерации графика: ${e.message}")
        e.printStackTrace()
    }
}