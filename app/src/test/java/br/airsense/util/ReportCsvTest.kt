package br.airsense.util

import br.airsense.model.AirQualityReading
import br.airsense.model.AirSenseDevice
import br.airsense.model.Environment
import br.airsense.ui.viewmodel.MetricSummary
import br.airsense.ui.viewmodel.ReportFilter
import br.airsense.ui.viewmodel.ReportResult
import br.airsense.ui.viewmodel.ReportRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReportCsvTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val env = Environment(id = 1, userId = "u1", name = "Casa")
    private val device = AirSenseDevice(id = 5, userId = "u1", name = "Sala; janela", deviceId = "DEV-1", environmentId = 1)
    private val reading = AirQualityReading(
        id = 1, deviceId = "DEV-1", temperature = 22.5f, humidity = 48f,
        airQualityIndex = 35f, co2Level = 640f,
        timestamp = LocalDate.of(2026, 11, 2).atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
    )
    private val result = ReportResult(
        filter = ReportFilter(LocalDate.of(2026, 10, 27), LocalDate.of(2026, 11, 2), environmentId = 1),
        rows = listOf(ReportRow(reading, device.name, outOfRange = false)),
        aqi = MetricSummary(35f, 35f, 35f),
        temperature = MetricSummary(22.5f, 22.5f, 22.5f),
        humidity = MetricSummary(48f, 48f, 48f),
        co2 = MetricSummary(640f, 640f, 640f),
        outOfRangeCount = 0
    )

    private val csv = ReportCsv.build(result, listOf(env), emptyList(), listOf(device), zone)
    private val lines = csv.removePrefix("﻿").split("\r\n")

    @Test
    fun `comeca com BOM para o Excel reconhecer UTF-8`() {
        assertTrue(csv.startsWith("﻿"))
    }

    @Test
    fun `cabecalho traz periodo e filtros da tela`() {
        assertTrue(lines.contains("Período;27/10/2026 a 02/11/2026"))
        assertTrue(lines.contains("Ambiente;Casa"))
        assertTrue(lines.contains("Cômodo;Todos"))
        assertTrue(lines.contains("Leituras;1"))
    }

    @Test
    fun `leitura usa virgula decimal, horario local e escapa o ponto e virgula do nome`() {
        val row = lines.first { it.startsWith("02/11/2026") }
        assertEquals("02/11/2026 14:00;\"Sala; janela\";DEV-1;35,0;22,5;48,0;640,0;Não", row)
    }

    @Test
    fun `nome do arquivo tem o periodo`() {
        assertEquals("airsense_relatorio_2026-10-27_a_2026-11-02.csv", ReportCsv.fileName(result))
    }
}
