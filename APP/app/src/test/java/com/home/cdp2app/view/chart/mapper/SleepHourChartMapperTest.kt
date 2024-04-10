package com.home.cdp2app.view.chart.mapper

import com.home.cdp2app.health.heart.entity.HeartRate
import com.home.cdp2app.health.sleep.entity.SleepHour
import com.home.cdp2app.view.chart.Chart
import com.home.cdp2app.view.chart.type.HealthCategory
import org.junit.Assert
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import java.time.Duration
import java.time.Instant

class SleepHourChartMapperTest {

    val chartMapper = SleepHourChartMapper()

    @Test
    fun TEST_MAP_TO_CHART() {
        val firstSleep = SleepHour(Instant.now().minusSeconds(1), Duration.ofHours(2)) //2시간 꿀잠
        val secondSleep = SleepHour(Instant.now().minusSeconds(1), Duration.ofMinutes(30)) //30분 낮잠 (0으로 표시될것으로 생각됨)
        val chart : Chart = chartMapper.convertToChart(listOf(firstSleep, secondSleep)) //chart로 매핑
        //데이터 일치하는지 확인
        Assert.assertEquals(2, chart.chartData.size)
        Assert.assertEquals(firstSleep.date, chart.chartData[0].time)
        Assert.assertEquals(firstSleep.duration.toHours().toDouble(), chart.chartData[0].data, 0.0)
        Assert.assertEquals(secondSleep.date, chart.chartData[1].time)
        Assert.assertEquals(secondSleep.duration.toHours().toDouble(), chart.chartData[1].data, 0.0)
        assertEquals(HealthCategory.SLEEP_HOUR, chart.type)
    }


    @Test
    fun TEST_EMPTY_MAP_TO_CHART() {
        val data = listOf<SleepHour>()
        val chart : Chart =
            org.junit.jupiter.api.assertDoesNotThrow { chartMapper.convertToChart(data) }
        Assert.assertEquals(0, chart.chartData.size)
        assertEquals(HealthCategory.SLEEP_HOUR, chart.type)

    }
}