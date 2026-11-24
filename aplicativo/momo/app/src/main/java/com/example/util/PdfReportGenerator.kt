package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.BabyProfile
import com.example.data.model.DiaperChange
import com.example.data.model.DiaperEvent
import com.example.data.model.ObservacaoCuidador
import com.example.data.model.ReportPeriod
import com.example.data.model.ReportSummary
import com.example.data.model.TipoEvento
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Gerador do Relatório de Acompanhamento Infantil em PDF do aplicativo MOMO.
 *
 * Características principais:
 * 1. Formato padrão A4 (595 x 842 pt) com tipografia limpa, acolhedora e altamente legível.
 * 2. Logomarca oficial MOMO desenhada no cabeçalho superior sem alterações.
 * 3. Identificação completa do paciente (nome, idade e peso) e período dinâmico analisado.
 * 4. Resumo geral com 5 métricas em cartões com ícones vetoriais delicados:
 *    - Urina (quantidade)
 *    - Fezes (quantidade)
 *    - Diarreia (quantidade + indicador discreto de dias distintos com ocorrência)
 *    - Trocas confirmadas (total)
 *    - Tempo médio entre trocas consecutivas
 * 5. Cartão de Destaque: "Dia com maior número de eventos" com quantidade total e detalhamento
 *    de urina, fezes e diarreia, respeitando regras de desempate e ausência de registros.
 * 6. Histórico detalhado cronológico com data, tipo de evento, origem (Cuidador vs Sensor) e notas.
 * 7. Paginação automática fluida (Página X de Y) com repetição de cabeçalho nas páginas seguintes.
 * 8. Sem gráficos de barras e sem linguagem diagnóstica; rodapé médico obrigatório em todas as páginas.
 */
object PdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 36f
    private const val CONTENT_WIDTH = 523f // 595 - (2 * 36)
    private const val FOOTER_TOP_Y = 806f

    // =========================================================================
    // PALETA OFICIAL MOMO (Conforme especificação de design)
    // =========================================================================
    private val COLOR_ORANGE_PRIMARY  = Color.rgb(0xF4, 0x94, 0x5E) // #F4945E (Laranja principal)
    private val COLOR_PEACH_LIGHT     = Color.rgb(0xF8, 0xA0, 0x68) // #F8A068 (Pêssego claro)
    private val COLOR_CREAM           = Color.rgb(0xF9, 0xEE, 0xDE) // #F9EEDE (Creme suave)
    private val COLOR_YELLOW_SOFT     = Color.rgb(0xFA, 0xDD, 0xA8) // #FADDA8 (Amarelo suave)
    private val COLOR_LEAF_GREEN      = Color.rgb(0xAF, 0xB7, 0x4F) // #AFB74F (Verde da folha)
    private val COLOR_DARK_GREEN      = Color.rgb(0xA7, 0xB0, 0x4A) // #A7B04A (Verde escuro)

    private val COLOR_TEXT_PRIMARY    = Color.rgb(45, 49, 66)       // #2D3142
    private val COLOR_TEXT_SECONDARY  = Color.rgb(107, 114, 128)    // #6B7280
    private val COLOR_BORDER          = Color.rgb(238, 232, 222)    // #EEE8DE suave
    private val COLOR_BORDER_SUBTLE   = Color.rgb(243, 244, 246)    // #F3F4F6
    private val COLOR_CARD_BG         = Color.rgb(253, 251, 247)    // #FDFBF7 suave
    private val COLOR_CARD_WHITE      = Color.rgb(255, 255, 255)
    private val COLOR_CARD_HIGHLIGHT  = Color.rgb(255, 248, 242)    // #FFF8F2

    // Cores específicas para os cartões de eventos
    private val COLOR_URINE_ACCENT    = Color.rgb(238, 138, 48)     // Laranja quente
    private val COLOR_URINE_BG        = Color.rgb(255, 246, 235)
    private val COLOR_FECES_ACCENT    = Color.rgb(180, 110, 40)     // Marrom acolhedor
    private val COLOR_FECES_BG        = Color.rgb(254, 245, 230)
    private val COLOR_DIARRHEA_ACCENT = Color.rgb(220, 53, 69)      // Alerta suave
    private val COLOR_DIARRHEA_BG     = Color.rgb(255, 238, 240)
    private val COLOR_SYNC_ACCENT     = Color.rgb(0xF4, 0x94, 0x5E) // MOMO Orange
    private val COLOR_SYNC_BG         = Color.rgb(255, 242, 234)
    private val COLOR_TIME_ACCENT     = Color.rgb(0xAF, 0xB7, 0x4F) // MOMO Leaf Green
    private val COLOR_TIME_BG         = Color.rgb(246, 248, 235)

    // =========================================================================
    // ENTRADA PRINCIPAL
    // =========================================================================

    fun generateAndGetUri(
        context: Context,
        babyProfile: BabyProfile?,
        summary: ReportSummary,
        period: ReportPeriod,
        periodChanges: List<DiaperChange>,
        periodEvents: List<DiaperEvent> = emptyList(),
        isDemoMode: Boolean = false,
        includeHistory: Boolean = false
    ): Uri {
        return MomoReportLayout.generate(context, babyProfile, period, periodChanges, periodEvents, isDemoMode, includeHistory)
    }

    // =========================================================================
    // RENDERIZAÇÃO DAS SEÇÕES
    // =========================================================================

    private fun drawDemoBanner(canvas: android.graphics.Canvas, y: Float) {
        val bgPaint = Paint().apply {
            color = COLOR_YELLOW_SOFT
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = COLOR_PEACH_LIGHT
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawRoundRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + 16f, 4f, 4f, bgPaint)
        canvas.drawRoundRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + 16f, 4f, 4f, borderPaint)
        canvas.drawText(
            "MODO DEMONSTRAÇÃO ACADÊMICA • REGISTROS SIMULADOS PARA APRESENTAÇÃO",
            MARGIN_X + (CONTENT_WIDTH / 2f),
            y + 11.5f,
            textPaint
        )
    }

    private fun drawHeaderWithLogo(
        canvas: android.graphics.Canvas,
        y: Float,
        logoBitmap: Bitmap?
    ) {
        val rightX = MARGIN_X + CONTENT_WIDTH

        // Logomarca original MOMO (Pêssego com fraldinha)
        val logoSize = 44f
        if (logoBitmap != null) {
            val destRect = RectF(MARGIN_X, y + 2f, MARGIN_X + logoSize, y + 2f + logoSize)
            val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(logoBitmap, null, destRect, bmpPaint)
        } else {
            // Fallback vetorial gracioso caso o bitmap não possa ser carregado
            drawPeachIcon(canvas, MARGIN_X + 22f, y + 24f, 20f)
        }

        // Título e identificação institucional
        val titleX = MARGIN_X + logoSize + 12f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ORANGE_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val instPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 7.5f
        }

        canvas.drawText("Relatório de Acompanhamento Infantil", titleX, y + 16f, titlePaint)
        canvas.drawText("MOMO • Monitoramento Inteligente de Fraldas Infantis", titleX, y + 29f, subPaint)
        canvas.drawText("Universidade Federal de Ouro Preto (UFOP)", titleX, y + 41f, instPaint)

        // Coluna da Direita: Emissão
        val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
        val emitLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 7.5f
            textAlign = Paint.Align.RIGHT
        }
        val emitValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("DATA E HORÁRIO DE EMISSÃO", rightX, y + 16f, emitLabelPaint)
        canvas.drawText(sdf.format(Date()), rightX, y + 29f, emitValPaint)

        // Linha divisória suave
        val linePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN_X, y + 52f, rightX, y + 52f, linePaint)
    }

    private fun drawBabyInfoCard(
        canvas: android.graphics.Canvas,
        y: Float,
        babyProfile: BabyProfile?
    ) {
        val cardPaint = Paint().apply {
            color = COLOR_CARD_BG
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.04f
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val height = 34f
        canvas.drawRoundRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + height, 6f, 6f, cardPaint)
        canvas.drawRoundRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + height, 6f, 6f, borderPaint)

        val nameText = babyProfile?.name?.trim()?.ifEmpty { "Não informado" } ?: "Não informado"
        val ageText = formatAgePtBr(babyProfile?.birthDateMillis)
        val weightText = formatWeightPtBr(babyProfile?.weightKg)

        val col1X = MARGIN_X + 14f
        val col2X = MARGIN_X + 210f
        val col3X = MARGIN_X + 370f

        // Coluna 1: Nome da Criança
        canvas.drawText("PACIENTE / CRIANÇA", col1X, y + 12f, labelPaint)
        canvas.drawText(nameText, col1X, y + 25f, valPaint)

        // Coluna 2: Idade
        canvas.drawText("IDADE", col2X, y + 12f, labelPaint)
        canvas.drawText(ageText, col2X, y + 25f, valPaint)

        // Coluna 3: Peso
        canvas.drawText("PESO REGISTRADO", col3X, y + 12f, labelPaint)
        canvas.drawText(weightText, col3X, y + 25f, valPaint)
    }

    private fun drawPeriodBanner(
        canvas: android.graphics.Canvas,
        y: Float,
        period: ReportPeriod
    ) {
        val bgPaint = Paint().apply {
            color = COLOR_CREAM
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ORANGE_PRIMARY
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val intervalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val height = 24f
        canvas.drawRoundRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + height, 6f, 6f, bgPaint)
        canvas.drawRoundRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + height, 6f, 6f, borderPaint)

        // Período selecionado
        val periodTitle = when (period) {
            ReportPeriod.TODAY -> "Registro de Hoje"
            ReportPeriod.DAYS_7 -> "Registro de 7 dias"
            ReportPeriod.DAYS_30 -> "Registro de 30 dias"
        }

        // Intervalo de datas calculado dinamicamente
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
        val now = System.currentTimeMillis()
        val intervalText = when (period) {
            ReportPeriod.TODAY -> {
                "Intervalo analisado: ${sdfDate.format(Date(now))}"
            }
            ReportPeriod.DAYS_7 -> {
                val start = now - (6L * 24 * 3600 * 1000)
                "Intervalo analisado: ${sdfDate.format(Date(start))} a ${sdfDate.format(Date(now))}"
            }
            ReportPeriod.DAYS_30 -> {
                val start = now - (29L * 24 * 3600 * 1000)
                "Intervalo analisado: ${sdfDate.format(Date(start))} a ${sdfDate.format(Date(now))}"
            }
        }

        canvas.drawText(periodTitle, MARGIN_X + 12f, y + 15.5f, titlePaint)
        canvas.drawText(intervalText, MARGIN_X + CONTENT_WIDTH - 12f, y + 15.5f, intervalPaint)
    }

    private fun drawGeneralSummaryCards(
        canvas: android.graphics.Canvas,
        y: Float,
        urineCount: Int,
        fecesCount: Int,
        diarrheaCount: Int,
        distinctDiarrheaDays: Int,
        totalChanges: Int,
        averageHours: Double
    ) {
        val gap = 8f

        // Linha 1: Urina, Fezes e Diarreia (3 colunas iguais)
        val w3 = (CONTENT_WIDTH - (2 * gap)) / 3f
        val h3 = 45f

        // 1. Urina
        drawMetricCard(
            canvas = canvas,
            x = MARGIN_X,
            y = y,
            w = w3,
            h = h3,
            title = "URINA",
            value = "$urineCount",
            subtitle = if (urineCount == 1) "1 registro no período" else "$urineCount registros no período",
            accentColor = COLOR_URINE_ACCENT,
            bgColor = COLOR_URINE_BG,
            iconType = IconType.DROP
        )

        // 2. Fezes
        drawMetricCard(
            canvas = canvas,
            x = MARGIN_X + w3 + gap,
            y = y,
            w = w3,
            h = h3,
            title = "FEZES",
            value = "$fecesCount",
            subtitle = if (fecesCount == 1) "1 registro no período" else "$fecesCount registros no período",
            accentColor = COLOR_FECES_ACCENT,
            bgColor = COLOR_FECES_BG,
            iconType = IconType.FECES
        )

        // 3. Diarreia (Com indicador discreto de dias distintos)
        val diarrheaSub = when {
            diarrheaCount == 0 -> "Não registrada no período"
            distinctDiarrheaDays == 1 -> "Registrada em 1 dia do período"
            else -> "Registrada em $distinctDiarrheaDays dias do período"
        }

        drawMetricCard(
            canvas = canvas,
            x = MARGIN_X + (2 * (w3 + gap)),
            y = y,
            w = w3,
            h = h3,
            title = "DIARREIA",
            value = "$diarrheaCount",
            subtitle = diarrheaSub,
            accentColor = COLOR_DIARRHEA_ACCENT,
            bgColor = COLOR_DIARRHEA_BG,
            iconType = IconType.ALERT
        )

        // Linha 2: Trocas Confirmadas e Tempo Médio (2 colunas)
        val y2 = y + h3 + 6f
        val w2 = (CONTENT_WIDTH - gap) / 2f
        val h2 = 43f

        // 4. Trocas Confirmadas
        drawMetricCard(
            canvas = canvas,
            x = MARGIN_X,
            y = y2,
            w = w2,
            h = h2,
            title = "TROCAS CONFIRMADAS",
            value = "$totalChanges",
            subtitle = if (totalChanges == 1) "1 troca registrada" else "$totalChanges trocas registradas",
            accentColor = COLOR_SYNC_ACCENT,
            bgColor = COLOR_SYNC_BG,
            iconType = IconType.DIAPER
        )

        // 5. Tempo Médio Entre Trocas
        val avgText = if (totalChanges >= 2 && averageHours > 0) {
            String.format(Locale.forLanguageTag("pt-BR"), "%.1f horas", averageHours)
        } else {
            "Dados insuficientes"
        }
        val avgSub = if (totalChanges >= 2) {
            "Calculado entre trocas consecutivas"
        } else {
            "Mínimo de 2 trocas no período"
        }

        drawMetricCard(
            canvas = canvas,
            x = MARGIN_X + w2 + gap,
            y = y2,
            w = w2,
            h = h2,
            title = "TEMPO MÉDIO ENTRE TROCAS",
            value = avgText,
            subtitle = avgSub,
            accentColor = COLOR_TIME_ACCENT,
            bgColor = COLOR_TIME_BG,
            iconType = IconType.CLOCK
        )
    }

    private fun drawMetricCard(
        canvas: android.graphics.Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        title: String,
        value: String,
        subtitle: String,
        accentColor: Int,
        bgColor: Int,
        iconType: IconType
    ) {
        val bgPaint = Paint().apply {
            color = COLOR_CARD_WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 6.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.04f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 7f
        }

        canvas.drawRoundRect(x, y, x + w, y + h, 6f, 6f, bgPaint)
        canvas.drawRoundRect(x, y, x + w, y + h, 6f, 6f, borderPaint)

        // Ícone vetorial delicado
        val iconCx = x + 18f
        val iconCy = y + (h / 2f)
        drawVectorIcon(canvas, iconCx, iconCy, 12f, accentColor, bgColor, iconType)

        val textX = x + 36f
        canvas.drawText(title, textX, y + 13f, titlePaint)
        canvas.drawText(value, textX, y + 28f, valuePaint)

        // Subtítulo descritivo
        val subY = y + 39f
        canvas.drawText(subtitle.take(38), textX, subY, subPaint)
    }

    private fun drawPeakDayHighlightCard(
        canvas: android.graphics.Canvas,
        y: Float,
        peak: PeakDayResult
    ) {
        val bgPaint = Paint().apply {
            color = COLOR_CARD_HIGHLIGHT
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = COLOR_PEACH_LIGHT
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ORANGE_PRIMARY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.04f
        }
        val mainValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tiePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val detailTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val cardH = 50f
        canvas.drawRoundRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + cardH, 6f, 6f, bgPaint)
        canvas.drawRoundRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + cardH, 6f, 6f, borderPaint)

        canvas.drawText("DIA COM MAIOR NÚMERO DE EVENTOS", MARGIN_X + 14f, y + 14f, titlePaint)

        when (peak) {
            is PeakDayResult.NoEvents -> {
                val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_TEXT_SECONDARY
                    textSize = 9f
                }
                canvas.drawText("Nenhum evento registrado no período analisado.", MARGIN_X + 14f, y + 32f, emptyPaint)
            }
            is PeakDayResult.Success -> {
                // Linha Principal: Ex: "26/10/2026 — 6 eventos"
                val eventLabel = if (peak.totalEvents == 1) "1 evento" else "${peak.totalEvents} eventos"
                val mainText = "${peak.dateFormatted} — $eventLabel"
                canvas.drawText(mainText, MARGIN_X + 14f, y + 30f, mainValPaint)

                if (peak.hasTie) {
                    canvas.drawText(
                        "* Houve empate no período; exibindo a data mais recente.",
                        MARGIN_X + 14f,
                        y + 42f,
                        tiePaint
                    )
                }

                // Detalhamento dos tipos na direita: 💧 X urinas | 💩 Y fezes | ⚠️ Z diarreia
                val detailRightX = MARGIN_X + CONTENT_WIDTH - 14f
                var pillX = detailRightX

                val dText = if (peak.diarrheaCount == 1) "1 diarreia" else "${peak.diarrheaCount} diarreias"
                val fText = if (peak.fecesCount == 1) "1 fezes" else "${peak.fecesCount} fezes"
                val uText = if (peak.urineCount == 1) "1 urina" else "${peak.urineCount} urinas"

                // Diarreia pill
                pillX = drawDetailBadge(canvas, pillX, y + 17f, dText, IconType.ALERT, COLOR_DIARRHEA_ACCENT, COLOR_DIARRHEA_BG)
                pillX -= 8f

                // Fezes pill
                pillX = drawDetailBadge(canvas, pillX, y + 17f, fText, IconType.FECES, COLOR_FECES_ACCENT, COLOR_FECES_BG)
                pillX -= 8f

                // Urina pill
                drawDetailBadge(canvas, pillX, y + 17f, uText, IconType.DROP, COLOR_URINE_ACCENT, COLOR_URINE_BG)
            }
        }
    }

    private fun drawDetailBadge(
        canvas: android.graphics.Canvas,
        rightX: Float,
        y: Float,
        text: String,
        iconType: IconType,
        accentColor: Int,
        bgColor: Int
    ): Float {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textWidth = textPaint.measureText(text)
        val badgeW = textWidth + 24f
        val badgeH = 18f
        val leftX = rightX - badgeW

        val bgPaint = Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        canvas.drawRoundRect(leftX, y, rightX, y + badgeH, 4f, 4f, bgPaint)
        canvas.drawRoundRect(leftX, y, rightX, y + badgeH, 4f, 4f, borderPaint)

        // Ícone pequeno
        drawVectorIcon(canvas, leftX + 9f, y + (badgeH / 2f), 5.5f, accentColor, bgColor, iconType)

        // Texto
        canvas.drawText(text, leftX + 18f, y + 12.5f, textPaint)

        return leftX
    }

    private fun drawExplanatoryNote(canvas: android.graphics.Canvas, y: Float) {
        val noteBg = Paint().apply {
            color = COLOR_CARD_BG
            style = Paint.Style.FILL
        }
        val noteBorder = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val noteText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 6.8f
        }

        canvas.drawRoundRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + 24f, 4f, 4f, noteBg)
        canvas.drawRoundRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + 24f, 4f, 4f, noteBorder)

        canvas.drawText(
            "Nota explicativa: Os registros acima refletem as trocas confirmadas pelo cuidador e os eventos detectados pelo monitor MOMO.",
            MARGIN_X + 10f,
            y + 10.5f,
            noteText
        )
        canvas.drawText(
            "Uma troca pode conter mais de uma eliminação (ex: urina e fezes combinadas). O total de trocas não é somado como eliminação adicional.",
            MARGIN_X + 10f,
            y + 19.5f,
            noteText
        )
    }

    private fun drawHistoryTableHeader(
        canvas: android.graphics.Canvas,
        y: Float,
        isContinuation: Boolean
    ) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 7f
        }
        val headBgPaint = Paint().apply {
            color = COLOR_CREAM
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val colHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        if (!isContinuation) {
            canvas.drawText("HISTÓRICO DETALHADO DOS REGISTROS", MARGIN_X, y + 10f, titlePaint)
            canvas.drawText(
                "Registros cronológicos no período (do mais recente ao mais antigo):",
                MARGIN_X,
                y + 20f,
                subPaint
            )
        }

        val tableHeaderY = if (isContinuation) y else y + 25f
        val tableHeaderH = 18f

        canvas.drawRoundRect(MARGIN_X, tableHeaderY, MARGIN_X + CONTENT_WIDTH, tableHeaderY + tableHeaderH, 4f, 4f, headBgPaint)
        canvas.drawRoundRect(MARGIN_X, tableHeaderY, MARGIN_X + CONTENT_WIDTH, tableHeaderY + tableHeaderH, 4f, 4f, borderPaint)

        // Colunas com largura calibrada: Data e Hora | Tipo de Evento | Origem | Observações
        canvas.drawText("DATA E HORA", MARGIN_X + 8f, tableHeaderY + 12f, colHeadPaint)
        canvas.drawText("TIPO DE EVENTO", MARGIN_X + 90f, tableHeaderY + 12f, colHeadPaint)
        canvas.drawText("ORIGEM", MARGIN_X + 215f, tableHeaderY + 12f, colHeadPaint)
        canvas.drawText("OBSERVAÇÕES", MARGIN_X + 295f, tableHeaderY + 12f, colHeadPaint)
    }

    private fun drawHistoryTableRow(
        canvas: android.graphics.Canvas,
        y: Float,
        record: DetailedHistoryRow,
        isAlt: Boolean
    ) {
        val rowH = 20f
        if (isAlt) {
            val altPaint = Paint().apply {
                color = COLOR_CARD_BG
                style = Paint.Style.FILL
            }
            canvas.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + rowH, altPaint)
        }

        val linePaint = Paint().apply {
            color = COLOR_BORDER_SUBTLE
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN_X, y + rowH, MARGIN_X + CONTENT_WIDTH, y + rowH, linePaint)

        // 1. Data e Hora
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 7.2f
        }

        canvas.drawText(record.timeFormatted, MARGIN_X + 8f, y + 13f, timePaint)
        canvas.drawText(record.dateFormatted, MARGIN_X + 38f, y + 13f, datePaint)

        // 2. Tipo de Evento (Badge arredondado com cor de destaque)
        val badgeX = MARGIN_X + 90f
        val badgeY = y + 3.5f
        val badgeW = 112f
        val badgeH = 13f

        val (badgeBg, badgeText) = getEventTypeColors(record.eventType)
        val badgeBgPaint = Paint().apply {
            color = badgeBg
            style = Paint.Style.FILL
        }
        val badgeBorderPaint = Paint().apply {
            color = badgeText
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeText
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawRoundRect(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 3f, 3f, badgeBgPaint)
        canvas.drawRoundRect(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 3f, 3f, badgeBorderPaint)
        canvas.drawText(record.eventType.take(22), badgeX + 5f, badgeY + 9.5f, badgeTextPaint)

        // 3. Origem (Cuidador vs Sensor)
        val originX = MARGIN_X + 215f
        val originY = y + 3.5f
        val originW = 68f
        val originH = 13f

        val (origBg, origText) = if (record.isCaregiver) {
            Pair(COLOR_SYNC_BG, COLOR_ORANGE_PRIMARY)
        } else {
            Pair(COLOR_TIME_BG, COLOR_DARK_GREEN)
        }

        val origBgPaint = Paint().apply {
            color = origBg
            style = Paint.Style.FILL
        }
        val origBorderPaint = Paint().apply {
            color = origText
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
        val origTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = origText
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawRoundRect(originX, originY, originX + originW, originY + originH, 3f, 3f, origBgPaint)
        canvas.drawRoundRect(originX, originY, originX + originW, originY + originH, 3f, 3f, origBorderPaint)
        canvas.drawText(record.origin, originX + (originW / 2f), originY + 9.5f, origTextPaint)

        // 4. Observações
        val notesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (record.observation == "—") COLOR_TEXT_SECONDARY else COLOR_TEXT_PRIMARY
            textSize = 7.5f
        }
        canvas.drawText(record.observation.take(48), MARGIN_X + 295f, y + 13f, notesPaint)
    }

    private fun drawEmptyHistoryMessage(canvas: android.graphics.Canvas, y: Float) {
        val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Nenhum registro encontrado para o período selecionado.",
            MARGIN_X + (CONTENT_WIDTH / 2f),
            y + 20f,
            emptyPaint
        )
    }

    private fun drawSubsequentPageHeader(
        canvas: android.graphics.Canvas,
        y: Float,
        babyProfile: BabyProfile?,
        period: ReportPeriod
    ) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ORANGE_PRIMARY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 7.5f
            textAlign = Paint.Align.RIGHT
        }
        val linePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 1f
        }

        canvas.drawText("MOMO • Relatório de Acompanhamento Infantil (Continuação)", MARGIN_X, y + 10f, titlePaint)
        val nameText = babyProfile?.name?.trim()?.ifEmpty { "Não informado" } ?: "Não informado"
        canvas.drawText("Paciente: $nameText   |   Período: ${period.label}", MARGIN_X + CONTENT_WIDTH, y + 10f, metaPaint)
        canvas.drawLine(MARGIN_X, y + 18f, MARGIN_X + CONTENT_WIDTH, y + 18f, linePaint)
    }

    private fun drawFooter(canvas: android.graphics.Canvas, pageIndex: Int, totalPages: Int) {
        val linePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 1f
        }
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 7f
        }
        val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val disclaimerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }

        // Linha divisória superior do rodapé
        canvas.drawLine(MARGIN_X, FOOTER_TOP_Y, MARGIN_X + CONTENT_WIDTH, FOOTER_TOP_Y, linePaint)

        // Linha 1 do rodapé: Institucional e Numeração de Página
        canvas.drawText("MOMO — Monitor Inteligente de Fraldas Infantis • UFOP", MARGIN_X, FOOTER_TOP_Y + 11f, brandPaint)
        canvas.drawText("Página $pageIndex de $totalPages", MARGIN_X + CONTENT_WIDTH, FOOTER_TOP_Y + 11f, pagePaint)

        // Linha 2 do rodapé: Observação médica mandatória (Conforme Requisito 9)
        canvas.drawText(
            "Relatório gerado pelo MOMO para acompanhamento dos registros da criança. As informações não substituem a avaliação de um profissional de saúde.",
            MARGIN_X + (CONTENT_WIDTH / 2f),
            FOOTER_TOP_Y + 22f,
            disclaimerPaint
        )
    }

    // =========================================================================
    // ÍCONES VETORIAIS DELICADOS E FOFOS (Conforme Requisito 6)
    // =========================================================================

    private enum class IconType { DROP, FECES, ALERT, DIAPER, CLOCK }

    private fun drawVectorIcon(
        canvas: android.graphics.Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        accentColor: Int,
        bgColor: Int,
        type: IconType
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 1.3f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }

        val r = radius * 0.72f

        when (type) {
            IconType.DROP -> {
                // Gotinha de urina arredondada e fofa
                val dropPath = Path().apply {
                    moveTo(cx, cy - r * 0.85f)
                    cubicTo(cx - r * 0.75f, cy + r * 0.1f, cx - r * 0.7f, cy + r * 0.8f, cx, cy + r * 0.85f)
                    cubicTo(cx + r * 0.7f, cy + r * 0.8f, cx + r * 0.75f, cy + r * 0.1f, cx, cy - r * 0.85f)
                    close()
                }
                canvas.drawPath(dropPath, fillPaint)
            }
            IconType.FECES -> {
                // Ícone de cocô estilizado, simples e delicado (SEM usar folha)
                val fecesPath = Path().apply {
                    // Base inferior arredondada
                    moveTo(cx - r * 0.65f, cy + r * 0.55f)
                    cubicTo(cx - r * 0.8f, cy + r * 0.25f, cx - r * 0.5f, cy + r * 0.1f, cx - r * 0.4f, cy + r * 0.1f)
                    // Nível intermediário
                    cubicTo(cx - r * 0.6f, cy - r * 0.15f, cx - r * 0.2f, cy - r * 0.35f, cx - r * 0.1f, cy - r * 0.35f)
                    // Curva do topo
                    cubicTo(cx - r * 0.2f, cy - r * 0.65f, cx + r * 0.25f, cy - r * 0.75f, cx + r * 0.35f, cy - r * 0.55f)
                    cubicTo(cx + r * 0.15f, cy - r * 0.35f, cx + r * 0.25f, cy - r * 0.15f, cx + r * 0.35f, cy - r * 0.1f)
                    // Lado direito
                    cubicTo(cx + r * 0.65f, cy, cx + r * 0.55f, cy + r * 0.25f, cx + r * 0.5f, cy + r * 0.3f)
                    cubicTo(cx + r * 0.8f, cy + r * 0.4f, cx + r * 0.65f, cy + r * 0.65f, cx + r * 0.45f, cy + r * 0.68f)
                    // Fundo
                    cubicTo(cx + r * 0.2f, cy + r * 0.8f, cx - r * 0.2f, cy + r * 0.8f, cx - r * 0.65f, cy + r * 0.55f)
                    close()
                }
                canvas.drawPath(fecesPath, fillPaint)
            }
            IconType.ALERT -> {
                // Símbolo de atenção suave (visual distinto de fezes)
                val triPath = Path().apply {
                    moveTo(cx, cy - r * 0.75f)
                    lineTo(cx + r * 0.75f, cy + r * 0.65f)
                    lineTo(cx - r * 0.75f, cy + r * 0.65f)
                    close()
                }
                canvas.drawPath(triPath, fgPaint)
                // Haste de exclamação suave e ponto
                canvas.drawLine(cx, cy - r * 0.25f, cx, cy + r * 0.2f, fgPaint)
                canvas.drawCircle(cx, cy + r * 0.45f, 0.9f, fillPaint)
            }
            IconType.DIAPER -> {
                // Fraldinha estilizada com abas e cintura
                val diaperPath = Path().apply {
                    moveTo(cx - r * 0.65f, cy - r * 0.5f)
                    lineTo(cx + r * 0.65f, cy - r * 0.5f)
                    lineTo(cx + r * 0.6f, cy - r * 0.1f)
                    cubicTo(cx + r * 0.5f, cy + r * 0.35f, cx + r * 0.25f, cy + r * 0.65f, cx, cy + r * 0.65f)
                    cubicTo(cx - r * 0.25f, cy + r * 0.65f, cx - r * 0.5f, cy + r * 0.35f, cx - r * 0.6f, cy - r * 0.1f)
                    close()
                }
                canvas.drawPath(diaperPath, fillPaint)
                // Linha suave de contorno de aba
                val waistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = bgColor
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawLine(cx - r * 0.5f, cy - r * 0.35f, cx + r * 0.5f, cy - r * 0.35f, waistPaint)
            }
            IconType.CLOCK -> {
                // Relógio delicado para tempo médio
                canvas.drawCircle(cx, cy, r * 0.75f, fgPaint)
                canvas.drawLine(cx, cy, cx, cy - r * 0.45f, fgPaint)
                canvas.drawLine(cx, cy, cx + r * 0.35f, cy, fgPaint)
            }
        }
    }

    private fun drawPeachIcon(canvas: android.graphics.Canvas, cx: Float, cy: Float, radius: Float) {
        val peachPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ORANGE_PRIMARY
            style = Paint.Style.FILL
        }
        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_LEAF_GREEN
            style = Paint.Style.FILL
        }
        // Folhinha
        val leaf = Path().apply {
            moveTo(cx, cy - radius * 0.6f)
            cubicTo(cx + radius * 0.5f, cy - radius * 1.1f, cx + radius * 0.7f, cy - radius * 0.5f, cx, cy - radius * 0.4f)
            close()
        }
        canvas.drawPath(leaf, leafPaint)
        // Pêssego
        canvas.drawCircle(cx - radius * 0.25f, cy + radius * 0.1f, radius * 0.65f, peachPaint)
        canvas.drawCircle(cx + radius * 0.25f, cy + radius * 0.1f, radius * 0.65f, peachPaint)
    }

    // =========================================================================
    // CÁLCULOS DO DESTAQUE: DIA COM MAIOR NÚMERO DE EVENTOS
    // =========================================================================

    sealed class PeakDayResult {
        object NoEvents : PeakDayResult()
        data class Success(
            val dateFormatted: String,
            val totalEvents: Int,
            val urineCount: Int,
            val fecesCount: Int,
            val diarrheaCount: Int,
            val hasTie: Boolean
        ) : PeakDayResult()
    }

    private data class DayAggregation(
        val dateFormatted: String,
        val latestTimestamp: Long,
        var urineCount: Int = 0,
        var fecesCount: Int = 0,
        var diarrheaCount: Int = 0
    ) {
        val totalEvents: Int get() = urineCount + fecesCount + diarrheaCount
    }

    /**
     * Agrupa eventos por data local e identifica o dia com maior frequência de eliminações.
     * Regras:
     * 1. Apenas registros dentro do período selecionado.
     * 2. Agrupados por data local (dd/MM/yyyy).
     * 3. Eliminações somadas (urina + fezes + diarreia); trocas NÃO são somadas como eventos adicionais.
     * 4. Em caso de empate, apresenta o dia mais recente e sinaliza o empate.
     * 5. Sem registros, retorna NoEvents.
     */
    internal fun calculatePeakDay(
        changes: List<DiaperChange>,
        period: ReportPeriod
    ): PeakDayResult {
        if (changes.isEmpty()) return PeakDayResult.NoEvents

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
        val dayMap = mutableMapOf<String, DayAggregation>()

        for (change in changes) {
            val obs = change.observacaoCuidador
            val dayKey = sdf.format(Date(change.timestampMillis))
            val agg = dayMap.getOrPut(dayKey) {
                DayAggregation(dayKey, change.timestampMillis)
            }

            if (change.timestampMillis > agg.latestTimestamp) {
                // Atualiza com timestamp mais recente daquele dia para desempate
                dayMap[dayKey] = agg.copy(latestTimestamp = change.timestampMillis)
            }

            val currentAgg = dayMap[dayKey]!!
            if (obs.hasUrine) currentAgg.urineCount++
            if (obs.hasFeces) currentAgg.fecesCount++
            if (obs.hasDiarrhea) currentAgg.diarrheaCount++
        }

        // Filtra dias que tiveram pelo menos 1 evento biológico
        val daysWithEvents = dayMap.values.filter { it.totalEvents > 0 }
        if (daysWithEvents.isEmpty()) return PeakDayResult.NoEvents

        val maxEvents = daysWithEvents.maxOf { it.totalEvents }
        val daysWithMax = daysWithEvents.filter { it.totalEvents == maxEvents }

        // Em caso de empate, escolhe o dia cronologicamente mais recente
        val winner = daysWithMax.maxByOrNull { it.latestTimestamp } ?: daysWithMax.first()
        val hasTie = daysWithMax.size > 1

        return PeakDayResult.Success(
            dateFormatted = winner.dateFormatted,
            totalEvents = winner.totalEvents,
            urineCount = winner.urineCount,
            fecesCount = winner.fecesCount,
            diarrheaCount = winner.diarrheaCount,
            hasTie = hasTie
        )
    }

    // =========================================================================
    // ESTRUTURA UNIFICADA DO HISTÓRICO DETALHADO
    // =========================================================================

    data class DetailedHistoryRow(
        val timestampMillis: Long,
        val dateFormatted: String,
        val timeFormatted: String,
        val eventType: String,
        val origin: String,
        val observation: String,
        val isCaregiver: Boolean
    )

    internal fun buildDetailedRecords(
        changes: List<DiaperChange>,
        sensorEvents: List<DiaperEvent>
    ): List<DetailedHistoryRow> {
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
        val sdfTime = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR"))

        val rows = mutableListOf<DetailedHistoryRow>()

        // 1. Trocas confirmadas pelo Cuidador
        for (change in changes) {
            val dateObj = Date(change.timestampMillis)
            val eventTypeLabel = when (change.observacaoCuidador) {
                ObservacaoCuidador.URINA -> "Urina"
                ObservacaoCuidador.FEZES -> "Fezes"
                ObservacaoCuidador.URINA_E_FEZES -> "Urina e fezes"
                ObservacaoCuidador.DIARREIA -> "Diarreia"
                ObservacaoCuidador.URINA_E_DIARREIA -> "Urina e diarreia"
                ObservacaoCuidador.FRALDA_SECA -> "Fralda seca"
                ObservacaoCuidador.NAO_INFORMADO -> "Troca de fralda"
            }
            val note = change.notes?.trim()?.ifEmpty { "—" } ?: "—"

            rows.add(
                DetailedHistoryRow(
                    timestampMillis = change.timestampMillis,
                    dateFormatted = sdfDate.format(dateObj),
                    timeFormatted = sdfTime.format(dateObj),
                    eventType = eventTypeLabel,
                    origin = "Cuidador",
                    observation = note,
                    isCaregiver = true
                )
            )
        }

        // 2. Eventos automáticos do Sensor (excluindo espelho de troca para evitar duplicações)
        for (event in sensorEvents) {
            if (event.tipo == TipoEvento.TROCA) continue // Pula espelho de troca

            val dateObj = Date(event.timestampMillis)
            val eventTypeLabel = when (event.tipo) {
                TipoEvento.URINA -> "Urina detectada"
                TipoEvento.FEZES -> "Possível presença de fezes"
                TipoEvento.DIARREIA -> "Possível diarreia"
                TipoEvento.CALIBRACAO -> "Calibração (fralda nova)"
                else -> event.tipo.rotulo
            }
            val note = listOfNotNull(
                if (event.source == "reconstructed_test") "Teste reconstruído" else null,
                "Situação: ${event.situacao}",
                event.observacaoCuidador?.takeUnless { it == ObservacaoCuidador.NAO_INFORMADO }?.let { "Observado: ${it.rotulo}" },
                event.observacoes
            ).joinToString(" · ")

            rows.add(
                DetailedHistoryRow(
                    timestampMillis = event.timestampMillis,
                    dateFormatted = "",
                    timeFormatted = "",
                    eventType = eventTypeLabel,
                    origin = if (event.source == "reconstructed_test") "Teste reconstruído" else "Sensor",
                    observation = note,
                    isCaregiver = false
                )
            )
        }

        // Ordena cronologicamente decrescente (mais recente primeiro)
        return rows.sortedByDescending { it.timestampMillis }
    }

    private fun getEventTypeColors(eventType: String): Pair<Int, Int> {
        val lower = eventType.lowercase()
        return when {
            lower.contains("diarreia") -> Pair(COLOR_DIARRHEA_BG, COLOR_DIARRHEA_ACCENT)
            lower.contains("fezes") && lower.contains("urina") -> Pair(COLOR_CREAM, COLOR_ORANGE_PRIMARY)
            lower.contains("fezes") -> Pair(COLOR_FECES_BG, COLOR_FECES_ACCENT)
            lower.contains("urina") -> Pair(COLOR_URINE_BG, COLOR_URINE_ACCENT)
            lower.contains("calibra") -> Pair(COLOR_TIME_BG, COLOR_DARK_GREEN)
            else -> Pair(Color.rgb(243, 244, 246), Color.rgb(107, 114, 128))
        }
    }

    // =========================================================================
    // FORMATAÇÃO AUXILIAR
    // =========================================================================

    internal fun formatAgePtBr(birthDateMillis: Long?): String {
        if (birthDateMillis == null || birthDateMillis <= 0L) return "Não informada"
        val diff = System.currentTimeMillis() - birthDateMillis
        if (diff < 0) return "Recém-nascido"

        val days = diff / (1000L * 60 * 60 * 24)
        val months = (days / 30.4375).toInt()
        val years = months / 12

        return when {
            days < 30 -> "$days ${if (days == 1L) "dia" else "dias"}"
            months < 12 -> "$months ${if (months == 1) "mês" else "meses"}"
            else -> {
                val remMonths = months % 12
                if (remMonths == 0) {
                    "$years ${if (years == 1) "ano" else "anos"}"
                } else {
                    "$years ${if (years == 1) "ano" else "anos"} e $remMonths ${if (remMonths == 1) "mês" else "meses"}"
                }
            }
        }
    }

    internal fun formatWeightPtBr(weightKg: Float?): String {
        if (weightKg == null || weightKg <= 0f) return "Não informado"
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f kg", weightKg)
    }
}
