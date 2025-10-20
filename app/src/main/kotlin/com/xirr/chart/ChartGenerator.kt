package com.xirr.chart

import com.xirr.models.ReturnPoint
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.Styler
import org.knowm.xchart.style.markers.SeriesMarkers
import java.awt.Color
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Генератор графиков доходности
 */
class ChartGenerator {
    
    /**
     * Генерирует PNG-изображение графика доходности
     * 
     * @param isCumulative true = кумулятивная доходность за период, false = годовая XIRR
     */
    fun generateChart(
        returnPoints: List<ReturnPoint>, 
        outputPath: String = "xirr_chart.png",
        isCumulative: Boolean = false
    ): File {
        if (returnPoints.isEmpty()) {
            throw IllegalArgumentException("Список точек доходности пуст")
        }
        
        // Конвертируем даты в java.util.Date для XChart
        val xData = returnPoints.map { 
            java.util.Date.from(it.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
        }
        val yData = returnPoints.map { it.xirrRate * 100 }
        
        val title = if (isCumulative) "График кумулятивной доходности (за период)" else "График доходности XIRR (годовых)"
        val yAxisTitle = if (isCumulative) "Доходность за период (%)" else "Годовая доходность (%)"
        
        val chart = XYChartBuilder()
            .width(1200)
            .height(700)
            .title(title)
            .xAxisTitle("Дата")
            .yAxisTitle(yAxisTitle)
            .build()
        
        styleChart(chart, returnPoints, isCumulative)
        
        val seriesName = if (isCumulative) "Кумулятивная доходность" else "XIRR доходность"
        val series = chart.addSeries(seriesName, xData, yData)
        series.marker = SeriesMarkers.CIRCLE
        series.lineColor = if (isCumulative) Color(46, 204, 113) else Color(41, 128, 185)
        series.markerColor = if (isCumulative) Color(46, 204, 113) else Color(41, 128, 185)
        series.lineStyle = org.knowm.xchart.style.lines.SeriesLines.SOLID
        
        // Нулевая линия
        val zeroLine = chart.addSeries(
            "Нулевой уровень",
            xData,
            List(xData.size) { 0.0 }
        )
        zeroLine.lineColor = Color.GRAY
        zeroLine.lineStyle = org.knowm.xchart.style.lines.SeriesLines.DASH_DASH
        zeroLine.marker = SeriesMarkers.NONE
        
        val outputFile = File(outputPath)
        BitmapEncoder.saveBitmap(chart, outputFile.absolutePath, BitmapEncoder.BitmapFormat.PNG)
        
        return outputFile
    }
    
    private fun styleChart(chart: XYChart, returnPoints: List<ReturnPoint>, isCumulative: Boolean) {
        val styler = chart.styler
        
        styler.chartBackgroundColor = Color.WHITE
        styler.plotBackgroundColor = Color.WHITE
        styler.legendPosition = Styler.LegendPosition.InsideNE
        
        styler.isPlotGridLinesVisible = true
        styler.plotGridLinesColor = Color(220, 220, 220)
        
        styler.axisTickLabelsFont = java.awt.Font("Arial", java.awt.Font.PLAIN, 12)
        styler.yAxisDecimalPattern = "#0.00'%'"
        styler.markerSize = 6
        
        // ⭐ КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: настройка оси X для дат
        styler.datePattern = getDatePattern(returnPoints)
        styler.isXAxisTicksVisible = true
        
        // Поворот меток дат для лучшей читаемости
        styler.xAxisLabelRotation = 45
        
        // Автоматическая настройка количества меток на оси X
        val dataPointsCount = returnPoints.size
        when {
            dataPointsCount < 30 -> styler.xAxisTickMarkSpacingHint = 50  // Показываем больше меток
            dataPointsCount < 180 -> styler.xAxisTickMarkSpacingHint = 80  // Средняя плотность
            dataPointsCount < 365 -> styler.xAxisTickMarkSpacingHint = 100 // Меньше меток
            else -> styler.xAxisTickMarkSpacingHint = 120 // Еще меньше для многолетних данных
        }
    }
    
    /**
     * Определяет оптимальный формат дат в зависимости от длительности периода
     */
    private fun getDatePattern(returnPoints: List<ReturnPoint>): String {
        if (returnPoints.size < 2) return "dd.MM.yyyy"
        
        val firstDate = returnPoints.first().date
        val lastDate = returnPoints.last().date
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(firstDate, lastDate)
        
        return when {
            totalDays < 60 -> "dd MMM"           // Меньше 2 месяцев: "15 янв"
            totalDays < 365 -> "dd MMM"          // Меньше года: "15 янв"
            totalDays < 730 -> "MMM yyyy"        // 1-2 года: "янв 2024"
            else -> "MMM yyyy"                    // Больше 2 лет: "янв 2024"
        }
    }
}