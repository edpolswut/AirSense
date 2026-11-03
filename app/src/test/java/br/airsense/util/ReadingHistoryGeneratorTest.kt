package br.airsense.util

import br.airsense.model.AirSenseDevice
import br.airsense.model.ReadingLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ReadingHistoryGeneratorTest {

    private val device = AirSenseDevice(id = 1, userId = "u1", name = "Sala", deviceId = "DEV-TESTE01", environmentId = 3, roomId = 7)
    private val now = 1_762_200_000_000L // instante fixo para o teste ser repetível
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val hour = 60 * 60 * 1000L

    @Test
    fun `gera uma leitura por hora nos ultimos 7 dias`() {
        val readings = ReadingHistoryGenerator.generate(device, now, days = 7, zone = zone)

        assertEquals(7 * 24, readings.size)
        assertTrue(readings.zipWithNext().all { (a, b) -> b.timestamp - a.timestamp == hour })
        assertTrue(readings.last().timestamp <= now)
        assertTrue(readings.first().timestamp > now - 7 * 24 * hour)
    }

    @Test
    fun `leituras ficam dentro dos limites fisicos e herdam a localizacao do dispositivo`() {
        val readings = ReadingHistoryGenerator.generate(device, now, zone = zone)

        readings.forEach {
            assertEquals("DEV-TESTE01", it.deviceId)
            assertEquals(3, it.environmentId)
            assertEquals(7, it.roomId)
            assertTrue(it.temperature in ReadingLimits.TEMP_BOUNDS)
            assertTrue(it.humidity in ReadingLimits.HUMIDITY_BOUNDS)
            assertTrue(it.airQualityIndex in ReadingLimits.AIR_QUALITY_BOUNDS)
            assertTrue(it.co2Level in ReadingLimits.CO2_BOUNDS)
        }
    }

    @Test
    fun `nao duplica horarios que ja tem leitura`() {
        val first = ReadingHistoryGenerator.generate(device, now, zone = zone)
        // Leituras existentes em minutos quebrados da mesma hora também contam.
        val existing = first.map { it.timestamp + 17 * 60 * 1000L }

        val second = ReadingHistoryGenerator.generate(device, now, existingTimestamps = existing, zone = zone)

        assertEquals(0, second.size)
    }

    @Test
    fun `completa so as horas que faltam`() {
        val first = ReadingHistoryGenerator.generate(device, now, zone = zone)
        val existing = first.drop(10).map { it.timestamp }

        val second = ReadingHistoryGenerator.generate(device, now, existingTimestamps = existing, zone = zone)

        assertEquals(first.take(10).map { it.timestamp }, second.map { it.timestamp })
    }

    @Test
    fun `co2 medio a noite e maior que de dia`() {
        val readings = ReadingHistoryGenerator.generate(device, now, days = 7, zone = zone)
        val byNight = readings.groupBy {
            val h = Instant.ofEpochMilli(it.timestamp).atZone(zone).hour
            h >= 18 || h < 7
        }

        val night = byNight.getValue(true).map { it.co2Level }.average()
        val day = byNight.getValue(false).map { it.co2Level }.average()
        assertTrue("noite=$night dia=$day", night > day)
    }
}
