package com.example.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/** A4 com medidas em pontos, quebra real de texto e paginação por altura disponível. */
internal object MomoReportLayout {
    private val ink = Color.rgb(101, 77, 67)
    private val muted = Color.rgb(132, 112, 100)
    private val orange = Color.rgb(193, 105, 66)
    private val cream = Color.rgb(253, 245, 234)
    private val line = Color.rgb(229, 224, 210)
    private val locale = Locale.forLanguageTag("pt-BR")

    internal fun startOfPeriod(period: ReportPeriod, end: Long): Long =
        HistoryPeriodPolicy.start(period, end)

    fun generate(context: Context, baby: BabyProfile?, period: ReportPeriod,
        changes: List<DiaperChange>, events: List<DiaperEvent>, demo: Boolean, includeHistory: Boolean = false): Uri {
        val end = System.currentTimeMillis()
        val start = startOfPeriod(period, end)
        val selected = changes.filter { it.timestampMillis in start..end }.sortedBy { it.timestampMillis }
        val records = if (includeHistory) PdfReportGenerator.buildDetailedRecords(selected, emptyList()) else emptyList()
        val date = SimpleDateFormat("dd/MM/yyyy", locale).apply { timeZone = TimeZone.getTimeZone("America/Sao_Paulo") }
        val stamp = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", locale).apply { timeZone = date.timeZone }
        val range = "${date.format(Date(start))} a ${date.format(Date(end))}"
        val report = Pages(baby?.name?.trim()?.ifEmpty { "Nome não informado" } ?: "Nome não informado", range, stamp.format(Date(end)), demo, BitmapFactory.decodeResource(context.resources, R.drawable.momo_2))
        report.profile(PdfReportGenerator.formatAgePtBr(baby?.birthDateMillis), PdfReportGenerator.formatWeightPtBr(baby?.weightKg))
        report.section("Resumo do período")
        val urine = selected.count { it.observacaoCuidador.hasUrine }
        val feces = selected.count { it.observacaoCuidador.hasFeces }
        val diarrhea = selected.count { it.observacaoCuidador.hasDiarrhea }
        report.metrics(listOf("Urina" to "$urine", "Fezes" to "$feces", "Diarreia" to "$diarrhea"))
        val gaps = selected.zipWithNext { a, b -> b.timestampMillis - a.timestampMillis }
        val average = if (gaps.isEmpty()) "Não calculável - registre ao menos duas trocas." else {
            val minutes = (gaps.average() / 60_000).toLong()
            val duration = if (minutes == 0L) "menos de 1 min" else if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"
            "$duration entre trocas registradas (${gaps.size} intervalo(s))."
        }
        report.paragraph("${selected.size} trocas confirmadas  |  Intervalo médio: $average", 9f)
        report.paragraph("Resumo baseado nas observações do cuidador. Uma troca pode conter mais de uma observação.", 8.5f)
        val daily = selected.groupBy { change ->
            Calendar.getInstance(date.timeZone).apply {
                timeInMillis = change.timestampMillis
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.toSortedMap()
        report.paragraph("${daily.size} dia(s) com trocas registradas no período.", 9f)
        report.section("Destaques do acompanhamento")
        fun highlight(label: String, count: (DiaperChange) -> Boolean, least: Boolean = false) {
            val counts = daily.mapValues { (_, rows) -> rows.count(count) }
            val eligible = if (least) counts else counts.filterValues { it > 0 }
            val message = when {
                least && daily.size < 2 -> "Registros insuficientes para comparar dias."
                eligible.isEmpty() -> "Nenhum registro desse tipo no período."
                else -> {
                    val value = if (least) eligible.values.minOrNull()!! else eligible.values.maxOrNull()!!
                    val dates = eligible.filterValues { it == value }.keys.joinToString(", ") { date.format(Date(it)) }
                    "$value registro(s) em $dates${if (eligible.values.count { it == value } > 1) " (empate)" else ""}."
                }
            }
            report.paragraph(label, 10f, true, ink)
            report.paragraph(message, 9f)
        }
        highlight("Mais registros de fezes", { it.observacaoCuidador.hasFeces })
        highlight("Mais registros de diarreia", { it.observacaoCuidador.hasDiarrhea })
        highlight("Menos registros de urina", { it.observacaoCuidador.hasUrine }, true)
        report.section("Resumo diário")
        report.dailyHeader()
        val cursor = Calendar.getInstance(date.timeZone).apply { timeInMillis = start }
        while (cursor.timeInMillis <= end) {
            val day = cursor.timeInMillis
            report.dailyRow(date.format(Date(day)), daily[day] ?: emptyList())
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        if (includeHistory) {
            report.newPage()
            report.section("Trocas confirmadas")
            report.paragraph("Mais recentes primeiro. Cuidador = troca confirmada; sensor = detecção automática.", 8.5f)
            report.tableHeader()
            if (records.isEmpty()) report.paragraph("Nenhum registro disponível neste período.", 11f)
            records.filter { it.isCaregiver }.forEach { report.record(it) }
        }
        val document = report.render()
        val directory = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(directory, "relatorio_momo_${if (includeHistory) "completo" else "resumo"}_${period.name.lowercase()}_${end}.pdf")
        try { file.outputStream().use { document.writeTo(it) } } finally { document.close() }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun drawSymbol(c: Canvas, x: Float, y: Float, size: Float, label: String, color: Int) {
        c.save(); c.translate(x, y); c.scale(size, size)
        val path = Path()
        when {
            label.contains("diarreia", true) -> {
                path.moveTo(.14f, .72f); path.cubicTo(.01f,.58f,.16f,.45f,.31f,.49f)
                path.cubicTo(.27f,.26f,.49f,.23f,.59f,.40f); path.cubicTo(.76f,.29f,.93f,.43f,.81f,.57f)
                path.cubicTo(1.02f,.61f,.95f,.82f,.75f,.81f); path.cubicTo(.60f,.97f,.44f,.84f,.39f,.82f)
                path.cubicTo(.25f,.91f,.10f,.85f,.14f,.72f); path.close()
            }
            label.contains("fezes", true) -> {
                path.moveTo(.20f,.84f); path.cubicTo(.04f,.83f,.06f,.63f,.23f,.60f)
                path.cubicTo(.13f,.47f,.27f,.36f,.39f,.36f); path.cubicTo(.33f,.24f,.57f,.22f,.57f,.07f)
                path.cubicTo(.77f,.20f,.72f,.34f,.64f,.39f); path.cubicTo(.84f,.41f,.89f,.52f,.78f,.61f)
                path.cubicTo(.98f,.66f,.96f,.85f,.78f,.85f); path.close()
            }
            label.contains("urina", true) -> {
                path.moveTo(.5f,.06f); path.cubicTo(.38f,.27f,.16f,.48f,.16f,.65f)
                path.cubicTo(.16f,1.02f,.84f,1.02f,.84f,.65f); path.cubicTo(.84f,.48f,.62f,.27f,.5f,.06f); path.close()
            }
            else -> { path.addRoundRect(RectF(.1f,.2f,.9f,.8f),.12f,.12f,Path.Direction.CW) }
        }
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; alpha = 25 })
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = .055f; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND })
        c.restore()
    }

    private class Pages(val name: String, val range: String, val emitted: String, val demo: Boolean, val logo: Bitmap?) {
        private val pages = mutableListOf<MutableList<(Canvas) -> Unit>>()
        private var y = 0f
        private val width = 507f
        private val left = 44f
        init { newPage() }
        private fun paint(size: Float, bold: Boolean = false, color: Int = ink) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        private fun add(action: (Canvas) -> Unit) { pages.last().add(action) }
        fun newPage() { pages.add(mutableListOf()); y = if (pages.size == 1) (if (demo) 160f else 142f) else (if (demo) 112f else 92f) }
        private fun space(height: Float) { if (y + height > 770f) newPage() }
        private fun text(value: String, x: Float, baseline: Float, size: Float = 10f, bold: Boolean = false, color: Int = ink) {
            add { it.drawText(value, x, baseline, paint(size, bold, color)) }
        }
        private fun wrapped(value: String, size: Float, available: Float = width): List<String> {
            val p = paint(size)
            return value.replace("\r", "").split('\n').flatMap { raw ->
                var rest = raw.trim(); val result = mutableListOf<String>()
                while (rest.isNotEmpty()) {
                    var count = p.breakText(rest, true, available, null).coerceAtLeast(1)
                    if (count < rest.length) {
                        val word = rest.lastIndexOf(' ', count - 1)
                        if (word > 0) count = word
                        else if (count > 1 && Character.isHighSurrogate(rest[count - 1])) count--
                    }
                    result.add(rest.take(count)); rest = rest.drop(count).trimStart()
                }
                result.ifEmpty { listOf("") }
            }
        }
        fun title(title: String, subtitle: String) {
            paragraph(title, 23f, true); paragraph(subtitle, 10f, false, orange)
        }
        fun paragraph(value: String, size: Float = 10f, bold: Boolean = false, color: Int = muted) {
            wrapped(value, size).forEach { part ->
                space(size * 1.5f); text(part, left, y + size, size, bold, color); y += size * 1.5f
            }; y += 8f
        }
        fun section(title: String) {
            space(62f); y += 8f; val top = y
            val pillWidth = paint(11f, true).measureText(title) + 24f
            add { it.drawRoundRect(RectF(left, top, left + pillWidth, top + 25f), 12f, 12f, paint(10f, color = Color.rgb(233, 237, 218))) }
            text(title, left + 12f, top + 17f, 11f, true, Color.rgb(78, 92, 58))
            add { it.drawLine(left + pillWidth + 12f, top + 13f, left + width, top + 13f, paint(1f, color = line)) }
            y += 36f
        }
        fun profile(age: String, weight: String) {
            val names = wrapped(name, 14f, 175f)
            val height = maxOf(68f, 40f + names.size * 18f); space(height + 10f); val top = y
            add { it.drawRoundRect(RectF(left, top, left + width, top + height), 12f, 12f, paint(10f, color = cream)) }
            text("CRIANÇA", left + 16f, top + 18f, 8f, true, muted)
            names.forEachIndexed { i, s -> text(s, left + 16f, top + 37f + i * 18f, 14f, true) }
            text("$age | $weight", left + 16f, top + height - 10f, 8f, color = muted)
            text("PERÍODO ANALISADO", left + 222f, top + 18f, 8f, true, muted)
            text(range, left + 222f, top + 36f, 10f, true)
            text("Gerado em $emitted", left + 222f, top + 54f, 8f, color = muted)
            y += height + 10f
        }
        fun note(value: String) {
            val lines = wrapped(value, 9f, width - 32f); val height = lines.size * 14f + 26f
            space(height + 10f); val top = y
            add { it.drawRoundRect(RectF(left, top, left + width, top + height), 10f, 10f, paint(10f, color = cream)) }
            lines.forEachIndexed { i, s -> text(s, left + 16f, top + 21f + i * 14f, 9f) }
            y += height + 10f
        }
        fun metrics(values: List<Pair<String, String>>) {
            space(102f); val top = y
            val colors = listOf(Color.rgb(89, 105, 63), Color.rgb(151, 94, 51), Color.rgb(135, 101, 155))
            val backgrounds = listOf(Color.rgb(233, 237, 218), Color.rgb(251, 230, 202), Color.rgb(240, 228, 246))
            add { it.drawRoundRect(RectF(left, top, left + width, top + 88f), 12f, 12f, paint(10f, color = Color.rgb(253, 249, 242))) }
            values.forEachIndexed { i, (label, value) ->
                val x = left + i * 169f
                add { c -> c.drawCircle(x + 39f, top + 44f, 26f, paint(10f, color = backgrounds[i])); drawSymbol(c, x + 24f, top + 28f, 30f, label, colors[i]) }
                text(label, x + 76f, top + 33f, 10f, color = muted)
                text(value, x + 76f, top + 64f, 26f, true, colors[i])
            }; y += 100f
        }
        fun tableHeader() {
            space(50f); val top = y
            add { it.drawRoundRect(RectF(left, top, left + width, top + 29f), 6f, 6f, paint(10f, color = Color.rgb(233, 237, 218))) }
            listOf("Data", "Hora", "Ocorrência", "Origem / confirmação").forEachIndexed { i, s -> text(s, left + listOf(12f, 90f, 145f, 355f)[i], top + 19f, 9f, true, Color.rgb(78, 92, 58)) }
            y += 30f
        }
        fun dailyHeader() {
            space(48f); val top = y
            add { it.drawRect(left, top, left + width, top + 26f, paint(10f, color = cream)) }
            listOf("Dia", "Trocas", "Urina", "Fezes", "Diarreia").forEachIndexed { i, value ->
                text(value, left + listOf(10f, 170f, 250f, 330f, 410f)[i], top + 17f, 10f, true)
            }; y += 28f
        }
        fun dailyRow(date: String, changes: List<DiaperChange>) {
            if (y + 26f > 770f) { newPage(); dailyHeader() }
            val top = y
            val values = if (changes.isEmpty()) listOf("Sem registro", "-", "-", "-") else listOf("${changes.size}", "${changes.count { it.observacaoCuidador.hasUrine }}", "${changes.count { it.observacaoCuidador.hasFeces }}", "${changes.count { it.observacaoCuidador.hasDiarrhea }}")
            text(date, left + 10f, top + 16f, 10f)
            values.forEachIndexed { i, value -> text(value, left + listOf(170f, 250f, 330f, 410f)[i], top + 16f, if (value == "Sem registro") 8f else 10f, color = muted) }
            add { it.drawLine(left, top + 24f, left + width, top + 24f, paint(1f, color = line)) }
            y += 26f
        }
        fun record(row: PdfReportGenerator.DetailedHistoryRow) {
            val eventLines = wrapped(row.eventType, 10f, 178f)
            val notes = row.observation.takeUnless { it == "—" || it.isBlank() }
            val noteLines = notes?.let { wrapped(it, 9f, width - 24f) } ?: emptyList()
            val chunks = if (noteLines.isEmpty()) listOf(emptyList()) else noteLines.chunked(28)
            chunks.forEachIndexed { index, chunk ->
                val height = maxOf(40f, eventLines.size * 14f + 16f) + if (chunk.isEmpty()) 0f else (chunk.size * 13f + 12f)
                if (y + height > 770f) { newPage(); tableHeader() }
                val top = y
                add { c -> c.drawRect(left, top, left + width, top + height, paint(10f, color = if (row.isCaregiver) Color.rgb(255, 251, 246) else Color.rgb(247, 248, 242)))
                    drawSymbol(c, left + 145f, top + 10f, 17f, row.eventType, if (row.eventType.contains("diarreia", true)) Color.rgb(135, 101, 155) else if (row.eventType.contains("fezes", true)) Color.rgb(151, 94, 51) else Color.rgb(89, 105, 63)) }
                text(row.dateFormatted, left + 12f, top + 24f, 9f)
                text(row.timeFormatted, left + 90f, top + 24f, 10f)
                eventLines.forEachIndexed { i, s -> text(s, left + 170f, top + 21f + i * 14f, 10f) }
                add { it.drawRoundRect(RectF(left + 350f, top + 9f, left + width - 10f, top + 31f), 10f, 10f, paint(10f, color = if (row.isCaregiver) Color.rgb(233, 237, 218) else Color.rgb(252, 232, 213))) }
                text(if (row.isCaregiver) "Cuidador · confirmado" else if (row.origin == "Teste reconstruído") "Teste reconstruído" else "Sensor · automático", left + 360f, top + 24f, 8f, color = if (row.isCaregiver) Color.rgb(78, 92, 58) else ink)
                var baseline = top + maxOf(40f, eventLines.size * 14f + 16f) + 4f
                chunk.forEach { text(it, left + 12f, baseline, 9f, color = muted); baseline += 13f }
                if (index > 0) text("Observação (continuação)", left + 12f, top + height - 3f, 7f, color = muted)
                add { it.drawLine(left, top + height, left + width, top + height, paint(1f, color = line)) }
                y += height
            }
            y += 3f
        }
        fun render(): PdfDocument {
            val doc = PdfDocument()
            pages.forEachIndexed { index, commands ->
                val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                val c = page.canvas
                if (index == 0) {
                    logo?.let { bitmap ->
                        val w = 178f
                        val h = w * bitmap.height.toFloat() / bitmap.width
                        c.drawBitmap(bitmap, null, RectF((595f - w) / 2, 26f, (595f + w) / 2, 26f + h), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    }
                    val heading = "Diário de acompanhamento"
                    c.drawText(heading, (595f - paint(19f).measureText(heading)) / 2, 116f, paint(19f))
                    c.drawLine(180f, 130f, 415f, 130f, paint(1f, color = line))
                    // Raminhos vetoriais inspirados na folhinha da marca, sem personagem alheio.
                    val leafPaint = paint(10f, color = Color.rgb(175, 183, 79))
                    listOf(70f, 500f).forEach { x ->
                        c.drawLine(x, 103f, x + 20f, 66f, paint(1f, color = Color.rgb(175, 183, 79)))
                        c.drawOval(RectF(x - 5f, 75f, x + 8f, 84f), leafPaint)
                        c.drawOval(RectF(x + 8f, 62f, x + 21f, 71f), leafPaint)
                    }
                } else {
                    logo?.let { bitmap -> c.drawBitmap(bitmap, null, RectF(left, 25f, left + 90f, 25f + 90f * bitmap.height.toFloat() / bitmap.width), Paint(Paint.FILTER_BITMAP_FLAG)) }
                    c.drawText("Diário de acompanhamento", left + 115f, 45f, paint(14f))
                    c.drawText("${name.take(45)} | $range", left, 78f, paint(8f, color = muted))
                }
                if (demo) c.drawText("DEMONSTRAÇÃO - DADOS SIMULADOS", left, if (index == 0) 151f else 102f, paint(10f, true, orange))
                commands.forEach { it(c) }
                c.drawLine(left, 787f, left + width, 787f, paint(1f, color = line))
                c.drawText("MOMO | Registros de acompanhamento", left, 804f, paint(9f, color = muted))
                val footer = "Página ${index + 1} de ${pages.size}"
                c.drawText(footer, left + width - paint(9f).measureText(footer), 804f, paint(9f, color = muted))
                c.drawText("As informações não substituem a avaliação de um profissional de saúde.", left, 821f, paint(8f, color = muted))
                doc.finishPage(page)
            }
            return doc
        }
    }
}
