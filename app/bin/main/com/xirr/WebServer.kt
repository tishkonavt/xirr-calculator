package com.xirr

import com.xirr.calculator.ReturnChartService
import com.xirr.chart.ChartGenerator
import com.xirr.data.GrpcPortfolioDataProvider
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.html.*
import java.io.File
import java.time.LocalDate

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                call.respondHtml {
                    head {
                        title { +"XIRR Calculator" }
                        style {
                            unsafe {
                                raw("""
                                    body { font-family: Arial, sans-serif; max-width: 800px; margin: 50px auto; padding: 20px; }
                                    h1 { color: #333; }
                                    form { background: #f5f5f5; padding: 20px; border-radius: 8px; }
                                    label { display: block; margin-top: 15px; font-weight: bold; }
                                    input { width: 100%; padding: 8px; margin-top: 5px; border: 1px solid #ddd; border-radius: 4px; }
                                    button { margin-top: 20px; padding: 10px 20px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }
                                    button:hover { background: #0056b3; }
                                    .charts { margin-top: 30px; }
                                    img { max-width: 100%; margin: 20px 0; border: 1px solid #ddd; }
                                """)
                            }
                        }
                    }
                    body {
                        h1 { +"📊 XIRR Calculator" }
                        form(action = "/calculate", method = FormMethod.post) {
                            label { +"gRPC Host:" }
                            input(type = InputType.text, name = "host") { value = "10.77.8.30" }
                            
                            label { +"gRPC Port:" }
                            input(type = InputType.number, name = "port") { value = "30903" }
                            
                            label { +"Authorization Token:" }
                            input(type = InputType.text, name = "token") { placeholder = "Bearer токен или пусто" }
                            
                            label { +"Account ID:" }
                            input(type = InputType.text, name = "accountId") { value = "885136" }
                            
                            label { +"Trade System Name:" }
                            input(type = InputType.text, name = "tradeSystemName") { value = "TR1" }
                            
                            label { +"From Date (YYYY-MM-DD):" }
                            input(type = InputType.date, name = "fromDate") { value = "2016-12-28" }
                            
                            label { +"To Date (YYYY-MM-DD):" }
                            input(type = InputType.date, name = "toDate") { value = LocalDate.now().toString() }
                            
                            label { +"Currency:" }
                            input(type = InputType.text, name = "currency") { value = "RUB" }
                            
                            button(type = ButtonType.submit) { +"Построить графики" }
                        }
                    }
                }
            }
            
            post("/calculate") {
                val params = call.receiveParameters()
                
                val host = params["host"]!!
                val port = params["port"]!!.toInt()
                val token = params["token"] ?: ""
                val accountId = params["accountId"]!!
                val tradeSystemName = params["tradeSystemName"]!!
                val fromDate = LocalDate.parse(params["fromDate"]!!)
                val toDate = LocalDate.parse(params["toDate"]!!)
                val currency = params["currency"]!!
                
                try {
                    // Получаем данные через gRPC
                    val dataProvider = GrpcPortfolioDataProvider(
                        host, port, token, accountId, tradeSystemName, fromDate, toDate, currency
                    )
                    val snapshots = dataProvider.getPortfolioSnapshots()
                    
                    if (snapshots.isEmpty()) {
                        call.respondText("Нет данных за указанный период", status = HttpStatusCode.BadRequest)
                        return@post
                    }
                    
                    // Рассчитываем доходности
                    val chartService = ReturnChartService()
                    val xirrPoints = chartService.buildReturnChart(snapshots)
                    val cumulativePoints = chartService.buildCumulativeReturnChart(snapshots)
                    
                    // Генерируем графики
                    val chartGenerator = ChartGenerator()
                    val xirrChart = chartGenerator.generateChart(xirrPoints, "xirr_chart.png", false)
                    val cumulativeChart = chartGenerator.generateChart(cumulativePoints, "cumulative_chart.png", true)
                    
                    // Показываем результат
                    call.respondHtml {
                        head {
                            title { +"XIRR Results" }
                            style {
                                unsafe {
                                    raw("""
                                        body { font-family: Arial, sans-serif; max-width: 1200px; margin: 50px auto; padding: 20px; }
                                        h1, h2 { color: #333; }
                                        .stats { background: #f5f5f5; padding: 15px; border-radius: 8px; margin: 20px 0; }
                                        img { max-width: 100%; margin: 20px 0; border: 1px solid #ddd; }
                                        a { display: inline-block; margin-top: 20px; padding: 10px 20px; background: #28a745; color: white; text-decoration: none; border-radius: 4px; }
                                        a:hover { background: #218838; }
                                    """)
                                }
                            }
                        }
                        body {
                            h1 { +"📊 Результаты расчёта" }
                            
                            div(classes = "stats") {
                                p { +"Период: ${snapshots.first().date} - ${snapshots.last().date}" }
                                p { +"Всего дней: ${snapshots.size}" }
                                p { +"Годовая доходность (XIRR): ${String.format("%.2f%%", xirrPoints.last().xirrRate * 100)}" }
                                p { +"Кумулятивная доходность: ${String.format("%.2f%%", cumulativePoints.last().xirrRate * 100)}" }
                            }
                            
                            h2 { +"График годовой доходности (XIRR)" }
                            img(src = "/chart/xirr", alt = "XIRR Chart")
                            
                            h2 { +"График кумулятивной доходности" }
                            img(src = "/chart/cumulative", alt = "Cumulative Chart")
                            
                            a(href = "/") { +"← Назад" }
                        }
                    }
                    
                } catch (e: Exception) {
                    call.respondText("Ошибка: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }
            
            get("/chart/{type}") {
                val type = call.parameters["type"]
                val fileName = if (type == "xirr") "xirr_chart.png" else "cumulative_chart.png"
                val file = File(fileName)
                
                if (file.exists()) {
                    call.respondFile(file)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }.start(wait = true)
    
    println("Сервер запущен на http://localhost:8080")
}