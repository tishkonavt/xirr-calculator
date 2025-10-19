package com.xirr.calculator

import com.xirr.models.CashFlow
import com.xirr.models.PortfolioSnapshot
import com.xirr.models.ReturnPoint
import com.xirr.models.XIRRResult
import java.time.temporal.ChronoUnit
import kotlin.math.pow

/**
 * Сервис для построения графика доходности портфеля
 */
class ReturnChartService(
    private val xirrCalculator: XIRRCalculator = XIRRCalculator()
) {
    
    /**
     * Строит график ГОДОВОЙ доходности (XIRR)
     */
    fun buildReturnChart(snapshots: List<PortfolioSnapshot>): List<ReturnPoint> {
        if (snapshots.isEmpty()) {
            return emptyList()
        }
        
        val sortedSnapshots = snapshots.sortedBy { it.date }
        val returnPoints = mutableListOf<ReturnPoint>()
        
        for (i in sortedSnapshots.indices) {
            val currentDate = sortedSnapshots[i].date
            val snapshotsUpToDate = sortedSnapshots.subList(0, i + 1)
            val cashFlows = convertToCashFlowsFixed(snapshotsUpToDate)
            val xirrResult = xirrCalculator.calculate(cashFlows)
            
            val rate = when (xirrResult) {
                is XIRRResult.Success -> xirrResult.rate
                is XIRRResult.Error -> {
                    if (i == 0) 0.0 else {
                        println("Предупреждение: не удалось рассчитать XIRR для даты $currentDate: ${xirrResult.message}")
                        0.0
                    }
                }
            }
            
            returnPoints.add(ReturnPoint(date = currentDate, xirrRate = rate))
        }
        
        return returnPoints
    }
    
    /**
     * Строит график КУМУЛЯТИВНОЙ доходности (за весь период)
     * На основе XIRR: кумулятивная = (1 + XIRR)^years - 1
     */
    fun buildCumulativeReturnChart(snapshots: List<PortfolioSnapshot>): List<ReturnPoint> {
        if (snapshots.isEmpty()) {
            return emptyList()
        }
        
        // Сначала получаем XIRR для каждой даты
        val xirrPoints = buildReturnChart(snapshots)
        
        // Конвертируем XIRR в кумулятивную доходность
        val startDate = snapshots.first().date
        
        val cumulativePoints = xirrPoints.map { xirrPoint ->
            val years = ChronoUnit.DAYS.between(startDate, xirrPoint.date) / 365.0
            
            // Кумулятивная доходность = (1 + XIRR)^years - 1
            val cumulativeReturn = if (years > 0) {
                (1.0 + xirrPoint.xirrRate).pow(years) - 1.0
            } else {
                0.0
            }
            
            ReturnPoint(date = xirrPoint.date, xirrRate = cumulativeReturn)
        }
        
        return cumulativePoints
    }
    
    /**
     * Конвертирует снимки портфеля в денежные потоки для XIRR
     */
    private fun convertToCashFlowsFixed(snapshots: List<PortfolioSnapshot>): List<CashFlow> {
        val cashFlows = mutableListOf<CashFlow>()
        
        snapshots.forEach { snapshot ->
            if (snapshot.cashIn > 0) {
                cashFlows.add(CashFlow(date = snapshot.date, amount = -snapshot.cashIn))
            }
            if (snapshot.cashOut > 0) {
                cashFlows.add(CashFlow(date = snapshot.date, amount = snapshot.cashOut))
            }
        }
        
        val lastSnapshot = snapshots.last()
        val netValuation = lastSnapshot.valuation - lastSnapshot.cashIn + lastSnapshot.cashOut
        
        cashFlows.add(CashFlow(date = lastSnapshot.date, amount = netValuation))
        
        return cashFlows
    }
    
    /**
     * Форматирует результат для вывода
     */
    fun formatReturnChart(returnPoints: List<ReturnPoint>, isCumulative: Boolean = false): String {
        val sb = StringBuilder()
        val title = if (isCumulative) "График кумулятивной доходности" else "График доходности XIRR"
        val unit = if (isCumulative) "(за период)" else "(годовых)"
        
        sb.appendLine("=== $title ===\n")
        sb.appendLine("Дата       | Доходность $unit")
        sb.appendLine("-----------|---------------")
        
        returnPoints.forEach { point ->
            val percentage = String.format("%.2f%%", point.xirrRate * 100)
            sb.appendLine("${point.date} | $percentage")
        }
        
        return sb.toString()
    }
}