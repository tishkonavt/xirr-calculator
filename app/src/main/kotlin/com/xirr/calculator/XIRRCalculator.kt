package com.xirr.calculator

import com.xirr.models.CashFlow
import com.xirr.models.XIRRResult
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow

/**
 * Калькулятор XIRR (Extended Internal Rate of Return)
 * Использует метод Ньютона для поиска ставки доходности
 */
class XIRRCalculator {
    
    companion object {
        private const val MAX_ITERATIONS = 100
        private const val PRECISION = 1e-6
        private const val INITIAL_GUESS = 0.1  // Начальное приближение 10%
    }
    
    /**
     * Рассчитывает XIRR для списка денежных потоков
     */
    fun calculate(cashFlows: List<CashFlow>): XIRRResult {
        // Валидация данных
        if (cashFlows.isEmpty()) {
            return XIRRResult.Error("Список денежных потоков пуст")
        }
        
        if (cashFlows.size < 2) {
            return XIRRResult.Error("Нужно минимум 2 транзакции для расчета XIRR")
        }
        
        // Проверка: должна быть минимум одна положительная и одна отрицательная сумма
        val hasPositive = cashFlows.any { it.amount > 0 }
        val hasNegative = cashFlows.any { it.amount < 0 }
        
        if (!hasPositive || !hasNegative) {
            return XIRRResult.Error("Должны быть и вложения (отрицательные), и возвраты (положительные)")
        }
        
        // Сортируем по дате
        val sortedFlows = cashFlows.sortedBy { it.date }
        val baseDate = sortedFlows.first().date
        
        // Метод Ньютона
        var rate = INITIAL_GUESS
        
        for (i in 0 until MAX_ITERATIONS) {
            val npv = calculateNPV(sortedFlows, baseDate, rate)
            val derivative = calculateDerivative(sortedFlows, baseDate, rate)
            
            if (abs(derivative) < 1e-10) {
                return XIRRResult.Error("Производная слишком мала, не удается найти решение")
            }
            
            val newRate = rate - npv / derivative
            
            // Проверка сходимости
            if (abs(newRate - rate) < PRECISION) {
                return XIRRResult.Success(newRate)
            }
            
            rate = newRate
            
            // Защита от ухода в отрицательные или слишком большие значения
            if (rate < -0.99 || rate > 10.0) {
                return XIRRResult.Error("Расчет не сходится, возможно некорректные данные")
            }
        }
        
        return XIRRResult.Error("Превышено максимальное количество итераций")
    }
    
    /**
     * Рассчитывает NPV (Net Present Value) для заданной ставки
     */
    private fun calculateNPV(
        cashFlows: List<CashFlow>,
        baseDate: LocalDate,
        rate: Double
    ): Double {
        return cashFlows.sumOf { flow ->
            val years = ChronoUnit.DAYS.between(baseDate, flow.date) / 365.0
            flow.amount / (1 + rate).pow(years)
        }
    }
    
    /**
     * Рассчитывает производную NPV по ставке
     */
    private fun calculateDerivative(
        cashFlows: List<CashFlow>,
        baseDate: LocalDate,
        rate: Double
    ): Double {
        return cashFlows.sumOf { flow ->
            val years = ChronoUnit.DAYS.between(baseDate, flow.date) / 365.0
            -years * flow.amount / (1 + rate).pow(years + 1)
        }
    }
}