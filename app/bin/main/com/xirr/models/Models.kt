package com.xirr.models

import java.time.LocalDate

/**
 * Денежный поток - транзакция с датой и суммой
 * amount: отрицательное значение = вложение, положительное = возврат
 */
data class CashFlow(
    val date: LocalDate,
    val amount: Double
)

/**
 * Снимок портфеля на конкретную дату
 * Данные, которые приходят от gRPC
 */
data class PortfolioSnapshot(
    val date: LocalDate,
    val valuation: Double,    // Оценка портфеля (текущая стоимость)
    val cashIn: Double,        // Ввод средств в этот день
    val cashOut: Double        // Вывод средств в этот день
)

/**
 * Точка на графике доходности
 */
data class ReturnPoint(
    val date: LocalDate,
    val xirrRate: Double       // Доходность XIRR на эту дату (годовая в долях, 0.1163 = 11.63%)
)

/**
 * Результат расчета XIRR
 */
sealed class XIRRResult {
    data class Success(val rate: Double) : XIRRResult()
    data class Error(val message: String) : XIRRResult()
}