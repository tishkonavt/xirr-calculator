package com.xirr.data

import com.xirr.models.PortfolioSnapshot
import java.time.LocalDate

/**
 * Интерфейс для получения данных о портфеле
 * В будущем здесь будет реализация через gRPC
 */
interface DataProvider {
    fun getPortfolioSnapshots(): List<PortfolioSnapshot>
}

/**
 * Временная mock-реализация для тестирования
 * Позже замените на gRPC клиент
 */
class MockDataProvider : DataProvider {
    override fun getPortfolioSnapshots(): List<PortfolioSnapshot> {
        return listOf(
            PortfolioSnapshot(
                date = LocalDate.of(2024, 1, 1),
                valuation = 10000.0,
                cashIn = 10000.0,
                cashOut = 0.0
            ),
            PortfolioSnapshot(
                date = LocalDate.of(2024, 6, 1),
                valuation = 14000.0,
                cashIn = 5000.0,
                cashOut = 0.0
            ),
            PortfolioSnapshot(
                date = LocalDate.of(2025, 1, 1),
                valuation = 16500.0,
                cashIn = 0.0,
                cashOut = 0.0
            )
        )
    }
}

/**
 * Будущая реализация через gRPC (заготовка)
 */
class GrpcDataProvider(
    private val host: String,
    private val port: Int
) : DataProvider {
    override fun getPortfolioSnapshots(): List<PortfolioSnapshot> {
        // TODO: Реализовать gRPC вызов
        throw NotImplementedError("gRPC integration not implemented yet")
    }
}