package com.xirr.calculator

import com.xirr.models.CashFlow
import com.xirr.models.PortfolioSnapshot
import com.xirr.models.ReturnPoint
import com.xirr.models.XIRRResult
import java.time.temporal.ChronoUnit
import kotlin.math.pow

class ReturnChartService(
    private val xirrCalculator: XIRRCalculator = XIRRCalculator()
) {
    
    fun buildReturnChart(snapshots: List<PortfolioSnapshot>): List<ReturnPoint> {
        if (snapshots.isEmpty()) {
            return emptyList()
        }
        
        val sortedSnapshots = snapshots.sortedBy { it.date }
        val returnPoints = mutableListOf<ReturnPoint>()
        
        for (i in sortedSnapshots.indices) {
            val currentDate = sortedSnapshots[i].date
            val snapshotsUpToDate = sortedSnapshots.subList(0, i + 1)
            val cashFlows = convertToCashFlows(snapshotsUpToDate)
            
            // Первый день всегда 0%
            if (i == 0) {
                returnPoints.add(ReturnPoint(date = currentDate, xirrRate = 0.0))
                continue
            }
            
            val xirrResult = xirrCalculator.calculate(cashFlows)
            
            val rate = when (xirrResult) {
                is XIRRResult.Success -> xirrResult.rate
                is XIRRResult.Error -> {
                    println("⚠️  XIRR не рассчитан для $currentDate: ${xirrResult.message}")
                    returnPoints.lastOrNull()?.xirrRate ?: 0.0
                }
            }
            
            returnPoints.add(ReturnPoint(date = currentDate, xirrRate = rate))
        }
        
        return returnPoints
    }
    
    fun buildCumulativeReturnChart(snapshots: List<PortfolioSnapshot>): List<ReturnPoint> {
        if (snapshots.isEmpty()) {
            return emptyList()
        }
        
        val xirrPoints = buildReturnChart(snapshots)
        val startDate = snapshots.first().date
        
        val cumulativePoints = xirrPoints.map { xirrPoint ->
            val years = ChronoUnit.DAYS.between(startDate, xirrPoint.date) / 365.0
            
            val cumulativeReturn = if (years > 0) {
                (1.0 + xirrPoint.xirrRate).pow(years) - 1.0
            } else {
                0.0
            }
            
            ReturnPoint(date = xirrPoint.date, xirrRate = cumulativeReturn)
        }
        
        return cumulativePoints
    }
    
    private fun convertToCashFlows(snapshots: List<PortfolioSnapshot>): List<CashFlow> {
        if (snapshots.isEmpty()) return emptyList()
        
        val cashFlows = mutableListOf<CashFlow>()
        val firstSnapshot = snapshots.first()
        
        // Первый день
        val initialInvestment = if (firstSnapshot.cashIn > 0) {
            firstSnapshot.cashIn
        } else if (firstSnapshot.valuation > 0) {
            firstSnapshot.valuation
        } else {
            0.0
        }
        
        if (initialInvestment > 0) {
            cashFlows.add(CashFlow(date = firstSnapshot.date, amount = -initialInvestment))
        }
        
        if (firstSnapshot.cashOut > 0) {
            cashFlows.add(CashFlow(date = firstSnapshot.date, amount = firstSnapshot.cashOut))
        }
        
        // Промежуточные дни (КРОМЕ последнего!)
        for (i in 1 until snapshots.size - 1) {
            val snapshot = snapshots[i]
            
            if (snapshot.cashIn > 0) {
                cashFlows.add(CashFlow(date = snapshot.date, amount = -snapshot.cashIn))
            }
            
            if (snapshot.cashOut > 0) {
                cashFlows.add(CashFlow(date = snapshot.date, amount = snapshot.cashOut))
            }
        }
        
        // Последний день - ТОЛЬКО чистая оценка
        if (snapshots.size > 1) {
            val lastSnapshot = snapshots.last()
            val netValue = lastSnapshot.valuation - lastSnapshot.cashIn + lastSnapshot.cashOut
            cashFlows.add(CashFlow(date = lastSnapshot.date, amount = netValue))
        } else {
            val netValue = firstSnapshot.valuation - firstSnapshot.cashIn + firstSnapshot.cashOut
            cashFlows.add(CashFlow(date = firstSnapshot.date, amount = netValue))
        }
        
        return cashFlows
    }
    
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