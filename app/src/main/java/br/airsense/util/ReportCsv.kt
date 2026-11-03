package br.airsense.util

import br.airsense.model.AirSenseDevice
import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import br.airsense.ui.viewmodel.MetricSummary
import br.airsense.ui.viewmodel.ReportResult
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Relatório em CSV (US016 / RNF016): os mesmos dados, período e filtros da tela.
 * Separador ";" e vírgula decimal, como o Excel em português espera; UTF-8 com BOM.
 */
object ReportCsv {
    private const val BOM = "﻿"
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    fun fileName(result: ReportResult): String =
        "airsense_relatorio_${result.filter.startDate}_a_${result.filter.endDate}.csv"

    fun build(
        result: ReportResult,
        environments: List<Environment>,
        rooms: List<EnvironmentRoom>,
        devices: List<AirSenseDevice>,
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val f = result.filter
        val sb = StringBuilder(BOM)
        fun line(vararg cells: Any?) {
            sb.append(cells.joinToString(";") { escape(it?.toString().orEmpty()) }).append("\r\n")
        }

        line("Relatório de qualidade do ar - AirSense")
        line("Período", "${f.startDate.format(dateFormat)} a ${f.endDate.format(dateFormat)}")
        line("Ambiente", environments.firstOrNull { it.id == f.environmentId }?.name ?: "Todos")
        line("Cômodo", rooms.firstOrNull { it.id == f.roomId }?.name ?: "Todos")
        line("Dispositivo", devices.firstOrNull { it.id == f.deviceId }?.name ?: "Todos")
        line("Leituras", result.rows.size)
        line("Fora da faixa segura", result.outOfRangeCount)
        line("Status pela média de AQI", result.status.label)
        line()
        line("Métrica", "Mínimo", "Média", "Máximo")
        fun metric(name: String, m: MetricSummary) = line(name, num(m.min), num(m.avg), num(m.max))
        metric("AQI", result.aqi)
        metric("Temperatura (°C)", result.temperature)
        metric("Umidade (%)", result.humidity)
        metric("CO2 (ppm)", result.co2)
        line()
        line("Data e hora", "Dispositivo", "Identificador", "AQI", "Temperatura (°C)", "Umidade (%)", "CO2 (ppm)", "Fora da faixa")
        result.rows.forEach { row ->
            val r = row.reading
            line(
                Instant.ofEpochMilli(r.timestamp).atZone(zone).format(dateTimeFormat),
                row.deviceName,
                r.deviceId,
                num(r.airQualityIndex),
                num(r.temperature),
                num(r.humidity),
                num(r.co2Level),
                if (row.outOfRange) "Sim" else "Não"
            )
        }
        return sb.toString()
    }

    private fun num(value: Float): String = String.format(Locale("pt", "BR"), "%.1f", value)

    /** Aspas quando o texto tem ";", aspas ou quebra de linha (nome de dispositivo, por exemplo). */
    private fun escape(text: String): String =
        if (text.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
}
