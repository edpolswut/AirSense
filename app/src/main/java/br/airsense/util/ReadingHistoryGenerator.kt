package br.airsense.util

import br.airsense.model.AirQualityReading
import br.airsense.model.AirSenseDevice
import br.airsense.model.ReadingLimits
import java.time.Instant
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Gera um histórico de leituras realista para demonstração (modo offline).
 *
 * Cada dispositivo ganha um "perfil" próprio (sorteado pelo identificador, para ser estável),
 * com ciclo de dia/noite, uma deriva lenta (passeio aleatório) e picos ocasionais
 * (ex.: cozinhar, janelas fechadas). Não apaga nada: horários que já têm leitura são pulados.
 */
object ReadingHistoryGenerator {
    const val DEFAULT_DAYS = 7
    private const val HOUR_MS = 60 * 60 * 1000L
    private const val SPIKE_CHANCE = 0.04

    /**
     * @param now fim do período (normalmente o instante atual).
     * @param existingTimestamps horários que já têm leitura desse dispositivo; a hora deles é pulada.
     */
    fun generate(
        device: AirSenseDevice,
        now: Long = System.currentTimeMillis(),
        days: Int = DEFAULT_DAYS,
        existingTimestamps: Collection<Long> = emptyList(),
        zone: ZoneId = ZoneId.systemDefault(),
        random: Random = Random(device.deviceId.lowercase().hashCode().toLong() xor now)
    ): List<AirQualityReading> {
        val lastHour = now - now % HOUR_MS
        val hours = days * 24
        val takenHours = existingTimestamps.map { it - it % HOUR_MS }.toHashSet()

        // Perfil do dispositivo: cômodos diferentes ficam com níveis diferentes.
        val profile = Random(device.deviceId.lowercase().hashCode())
        val baseTemp = 20f + profile.nextFloat() * 4f       // 20–24 °C
        val baseHumidity = 40f + profile.nextFloat() * 15f  // 40–55 %
        val baseCo2 = 450f + profile.nextFloat() * 200f     // 450–650 ppm
        val baseAqi = 20f + profile.nextFloat() * 30f       // 20–50

        var drift = 0f
        val readings = ArrayList<AirQualityReading>(hours)
        for (i in hours - 1 downTo 0) {
            val timestamp = lastHour - i * HOUR_MS
            // Deriva lenta, presa entre -1 e 1, para os valores não pularem de hora em hora.
            drift = (drift + (random.nextFloat() - 0.5f) * 0.3f).coerceIn(-1f, 1f)
            if (timestamp - timestamp % HOUR_MS in takenHours) continue

            val hourOfDay = Instant.ofEpochMilli(timestamp).atZone(zone).hour
            // Calor: pico às 15h. Ocupação: maior à noite (18h–7h), quando há gente em casa.
            val heat = cos((hourOfDay - 15) / 24.0 * 2 * PI).toFloat()
            val occupancy = if (hourOfDay >= 18 || hourOfDay < 7) 1f else 0.3f
            val spike = random.nextDouble() < SPIKE_CHANCE

            val temperature = baseTemp + 3f * heat + drift + noise(random, 0.4f)
            val humidity = baseHumidity - 6f * heat + 4f * drift + noise(random, 1.5f)
            val co2 = baseCo2 + 350f * occupancy + 120f * drift + noise(random, 40f) +
                if (spike) 500f + random.nextFloat() * 600f else 0f
            val aqi = baseAqi + 20f * occupancy + 12f * drift + noise(random, 5f) +
                if (spike) 60f + random.nextFloat() * 80f else 0f

            readings += AirQualityReading(
                deviceId = device.deviceId,
                environmentId = device.environmentId,
                roomId = device.roomId,
                temperature = round1(temperature.coerceIn(ReadingLimits.TEMP_BOUNDS)),
                humidity = round1(humidity.coerceIn(ReadingLimits.HUMIDITY_BOUNDS)),
                airQualityIndex = abs(aqi).coerceIn(ReadingLimits.AIR_QUALITY_BOUNDS).roundToInt().toFloat(),
                co2Level = co2.coerceIn(ReadingLimits.CO2_BOUNDS).roundToInt().toFloat(),
                timestamp = timestamp
            )
        }
        return readings
    }

    private fun noise(random: Random, amplitude: Float) = (random.nextFloat() * 2f - 1f) * amplitude

    private fun round1(value: Float) = (value * 10f).roundToInt() / 10f
}
