package com.example.mylife.lifebalance.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.mylife.lifebalance.data.*
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import android.graphics.drawable.VectorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.example.lifebalance.R


/**
 * Результат генерации PDF: URI для шаринга и файл для последующего удаления из кэша.
 */
data class PdfExportResult(val uri: Uri, val file: File)

/**
 * Data container for PDF export.
 * @param includeBalanceWheel включить раздел «Колесо баланса»
 * @param includeTasks включить раздел «Дела»
 * @param includeGoals включить раздел «Цели»
 * @param includeIdeas включить раздел «Идеи»
 */
data class PdfExportData(
    val spheres: List<LifeSphere>,
    val tasks: List<Task>,
    val goals: List<Goal>,
    val folders: List<IdeaFolder>,
    val notesWithoutFolder: List<IdeaNote>,
    val notesByFolder: Map<Long, List<IdeaNote>>,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val includeBalanceWheel: Boolean = true,
    val includeTasks: Boolean = true,
    val includeGoals: Boolean = true,
    val includeIdeas: Boolean = true
)

/**
 * Helper for generating PDF reports from LifeBalance data.
 */
object PdfExportHelper {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 50f
    private const val LINE_HEIGHT = 18f

    private const val TITLE_SIZE = 16f
    private const val SECTION_SIZE = 14f
    private const val BODY_SIZE = 11f
    private const val SMALL_SIZE = 9f

    private val dateFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())

    // Палитра цветов колеса баланса (как в BalanceWheel.kt)
    private val colorPalette = listOf(
        0xFFE53935.toInt(), // красный
        0xFFFB8C00.toInt(), // оранжевый
        0xFFFDD835.toInt(), // желтый
        0xFF7CB342.toInt(), // зеленый
        0xFF26A69A.toInt(), // бирюзовый
        0xFF42A5F5.toInt(), // голубой
        0xFF5C6BC0.toInt(), // синий
        0xFF8E24AA.toInt(), // фиолетовый
        0xFFEC407A.toInt(), // розовый
        0xFF78909C.toInt(), // серо-голубой
        0xFF8D6E63.toInt(), // коричневый
        0xFF66BB6A.toInt()  // светло-зеленый
    )

    private fun parseSphereColor(sphere: LifeSphere): Int {
        val color = colorPalette[sphere.colorIndex % colorPalette.size]
        return Color.argb(178, Color.red(color), Color.green(color), Color.blue(color)) // alpha 0.7
    }

    // ---------- Paints ----------

    private val titlePaint = Paint().apply {
        textSize = TITLE_SIZE
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val sectionPaint = Paint().apply {
        textSize = SECTION_SIZE
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val bodyPaint = Paint().apply {
        textSize = BODY_SIZE
        isAntiAlias = true
    }

    private val smallPaint = Paint().apply {
        textSize = SMALL_SIZE
        isAntiAlias = true
    }

    private val deadlinePaint = Paint().apply {
        textSize = BODY_SIZE
        color = Color.GRAY
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        isAntiAlias = true
    }

    /** All possible translations of "moved from goals" and "added from life balance" for PDF source-note detection. */
    private fun buildSourceNoteDescriptionsSet(context: Context): Set<String> {
        val locales = listOf(
            Locale.ENGLISH,
            Locale("ru"),
            Locale("de"),
            Locale("es"),
            Locale("fr"),
            Locale("uk"),
            Locale.getDefault()
        ).distinct()
        val set = mutableSetOf<String>()
        val config = Configuration(context.resources.configuration)
        for (locale in locales) {
            try {
                config.setLocale(locale)
                val localizedContext = context.createConfigurationContext(config)
                val res = localizedContext.resources
                set.add(res.getString(R.string.moved_from_goals))
                set.add(res.getString(R.string.added_from_life_balance))
            } catch (_: Exception) { /* skip locale if resource missing */ }
        }
        return set
    }

    /**
     * Generates a PDF file and returns its URI and File (for cleanup from cache).
     */
    fun generatePdf(context: Context, data: PdfExportData): Result<PdfExportResult> {
        return try {
            val document = PdfDocument()
            var pageNumber = 1
            var page = createPage(document, pageNumber++)
            var y = MARGIN

            // ===== TITLE =====
            y = drawTitle(page, context.getString(R.string.lifebalance_planner), y)

            // ===== PERIOD =====
            val periodText = if (data.startDate != null && data.endDate != null) {
                val periodDataPdf = context.getString(R.string.period)
                "$periodDataPdf: ${data.startDate.format(dateFormatter)} — ${data.endDate.format(dateFormatter)}"
            } else {
                context.getString(R.string.period_all_data)
            }
            val periodX = PAGE_WIDTH / 2f - bodyPaint.measureText(periodText) / 2f
            page.canvas.drawText(periodText, periodX, y, bodyPaint)
            y += LINE_HEIGHT * 0.9f + LINE_HEIGHT

            var sectionNum = 0

            // ===== КОЛЕСО БАЛАНСА =====
            if (data.includeBalanceWheel) {
                sectionNum++
                val wheelOfLifeBalance = context.getString(R.string.wheel_of_life_balance).uppercase(Locale.getDefault())
                y = drawSection(page, "$sectionNum. $wheelOfLifeBalance", y)

                if (data.spheres.isEmpty()) {
                    y = drawText(page, context.getString(R.string.no_data), bodyPaint, y)
                } else {
                    // --- Колесо баланса как в приложении (BalanceWheel.kt) ---
                    val centerX = PAGE_WIDTH / 2f
                    val centerY = y + 130f
                    val radius = 100f
                    val sortedSpheres = data.spheres.sortedBy { it.order }
                    val sectors = sortedSpheres.size
                    val angleStep = 360f / sectors

                    val strokePaint = Paint().apply {
                        style = Paint.Style.STROKE
                        isAntiAlias = true
                    }
                    val fillPaint = Paint().apply {
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }

                    // 1. Концентрические круги (шкала 0–10)
                    strokePaint.color = Color.argb(76, 211, 211, 211) // LightGray alpha 0.3
                    strokePaint.strokeWidth = 1f
                    for (i in 1..10) {
                        val circleRadius = radius * (i / 10f)
                        page.canvas.drawCircle(centerX, centerY, circleRadius, strokePaint)
                    }

                    // 2. Секторы по оценке (только если score > 0)
                    sortedSpheres.forEachIndexed { index, sphere ->
                        val startAngle = index * angleStep - 90f
                        val score = sphere.score.coerceIn(0, 10)
                        if (score > 0) {
                            val scoreRadius = radius * (score / 10f)
                            fillPaint.color = parseSphereColor(sphere)
                            page.canvas.drawArc(
                                centerX - scoreRadius,
                                centerY - scoreRadius,
                                centerX + scoreRadius,
                                centerY + scoreRadius,
                                startAngle,
                                angleStep,
                                true,
                                fillPaint
                            )
                        }
                    }

                    // 3. Линии границ секторов
                    strokePaint.color = Color.argb(127, 128, 128, 128) // Gray alpha 0.5
                    strokePaint.strokeWidth = 2f
                    sortedSpheres.forEachIndexed { index, sphere ->
                        val startAngle = index * angleStep - 90f
                        val startRad = Math.toRadians(startAngle.toDouble())
                        page.canvas.drawLine(
                            centerX,
                            centerY,
                            centerX + cos(startRad).toFloat() * radius,
                            centerY + sin(startRad).toFloat() * radius,
                            strokePaint
                        )
                    }

                    // 4. Центральный круг
                    fillPaint.color = Color.argb(255, 249, 249, 249) // surface-подобный
                    page.canvas.drawCircle(centerX, centerY, 5f, fillPaint)
                    strokePaint.color = Color.argb(127, 128, 128, 128)
                    strokePaint.strokeWidth = 1f
                    page.canvas.drawCircle(centerX, centerY, 5f, strokePaint)

                    // 5. Подписи секторов: название + оценка/10; оценка — цветом сектора
                    val labelPaint = Paint().apply {
                        textSize = 10f
                        isFakeBoldText = true
                        isAntiAlias = true
                        color = Color.BLACK
                    }
                    val labelRadius = radius + 22f
                    sortedSpheres.forEachIndexed { index, sphere ->
                        val labelAngle = index * 360f / sectors - 90f + 180f / sectors
                        val labelRad = Math.toRadians(labelAngle.toDouble())
                        val labelX = centerX + cos(labelRad).toFloat() * labelRadius
                        val labelY = centerY + sin(labelRad).toFloat() * labelRadius
                        val nameText = "${sphere.name.uppercase()} "
                        val scoreText = "${sphere.score}/10"
                        val nameWidth = labelPaint.measureText(nameText)
                        val scoreWidth = labelPaint.measureText(scoreText)
                        val totalWidth = nameWidth + scoreWidth
                        val startX = labelX - totalWidth / 2f
                        page.canvas.drawText(nameText, startX, labelY + 4f, labelPaint)
                        labelPaint.color = colorPalette[sphere.colorIndex % colorPalette.size]
                        page.canvas.drawText(scoreText, startX + nameWidth, labelY + 4f, labelPaint)
                        labelPaint.color = Color.BLACK
                    }

                    y += 2 * radius + 40f
                }
                y += LINE_HEIGHT
                y += LINE_HEIGHT
                y += LINE_HEIGHT
            }

            // ===== ЦЕЛИ =====
            if (data.includeGoals) {
                sectionNum++
                val mainGoals = context.getString(R.string.main_goals).uppercase(Locale.getDefault())
                y = drawSection(page, "$sectionNum. $mainGoals", y)

                if (data.goals.isEmpty()) {
                    y = drawText(page, context.getString(R.string.no_goals), bodyPaint, y)
                } else {
                    val sphereMap = data.spheres.associateBy { it.id.toLong() }
                    val sortedGoals = data.goals.sortedWith(
                        compareBy<Goal> { sphereMap[it.sphereId.toLong()]?.order ?: Int.MAX_VALUE }
                            .thenBy { it.text }
                    )

                    var previousSphereId: Long? = null
                    sortedGoals.forEach { goal ->
                        val currentSphereId = goal.sphereId.toLong()
                        if (previousSphereId != null && previousSphereId != currentSphereId) {
                            y += LINE_HEIGHT
                        }
                        previousSphereId = currentSphereId

                        val status = if (goal.checked) "◉" else "○"
                        val sphereName = sphereMap[currentSphereId]?.name ?: ""
                        val prefix = "$status "
                        val content = "$sphereName: ${goal.text}"
                        val deadlineSuffix = "(до ${goal.deadline.format(dateFormatter)})"

                        y = drawTextWithPrefix(
                            page,
                            prefix,
                            content,
                            bodyPaint,
                            y,
                            suffix = deadlineSuffix
                        )

                        val result = checkPageBreak(document, page, y, pageNumber)
                        page = result.first
                        y = result.second
                    }
                }

                y += LINE_HEIGHT
            }

            // ===== ДЕЛА =====
            if (data.includeTasks) {
                sectionNum++
                val dailyTasks = context.getString(R.string.daily_tasks)
                y = drawSection(page, "$sectionNum. $dailyTasks", y)

                if (data.tasks.isEmpty()) {
                    y = drawText(page, context.getString(R.string.no_tasks_for_the_selected_period), bodyPaint, y)
                } else {
                    val sphereMap = data.spheres.associateBy { it.id }
                    val sourceNoteDescriptions = buildSourceNoteDescriptionsSet(context)

                    data.tasks.sortedWith(
                        compareBy<Task> { it.date }
                            .thenBy { sphereMap[it.sphereId]?.name ?: "" }
                            .thenBy { it.title }
                    ).forEach { task ->
                        val status = if (task.isCompleted) "☑" else "☐"
                        val prefix = "$status ${task.date.format(dateFormatter)} — "
                        val isSourceNote = task.description in sourceNoteDescriptions
                        val suffix = if (isSourceNote) "(${task.description})" else null

                        y = drawTextWithPrefix(page, prefix, task.title, bodyPaint, y, suffix)

                        if (task.description.isNotBlank() && !isSourceNote) {
                            y = drawText(
                                page,
                                task.description,
                                smallPaint,
                                y,
                                indent = 16f
                            )
                        }

                        val result = checkPageBreak(document, page, y, pageNumber)
                        page = result.first
                        y = result.second
                    }
                }
                y += LINE_HEIGHT
            }

            // ===== ИДЕИ =====
            if (data.includeIdeas) {
                sectionNum++
                val ideasTitle = context.getString(R.string.ideas_title)
                y = drawSection(page, "$sectionNum. $ideasTitle", y)

                val groups = mutableListOf<Pair<String, List<IdeaNote>>>()
                if (data.notesWithoutFolder.isNotEmpty()) {
                    groups.add("Без папки" to data.notesWithoutFolder)
                }
                data.folders.forEach { folder ->
                    data.notesByFolder[folder.id]?.let { notes ->
                        if (notes.isNotEmpty()) groups.add(folder.name to notes)
                    }
                }

                if (groups.isEmpty()) {
                    y = drawText(page, context.getString(R.string.no_ideas), bodyPaint, y)
                } else {
                    groups.forEach { (folderName, notes) ->
                        y = drawText(page, "➤ $folderName", bodyPaint, y)
                        var yy = y
                        notes.forEach { note ->
                            val aiIndex = note.text.indexOf("AI:")
                            val mainText = if (aiIndex >= 0) note.text.substring(0, aiIndex).trim() else note.text
                            val aiText = if (aiIndex >= 0) note.text.substring(aiIndex).trim() else null

                            val noteBulletWidth = bodyPaint.measureText("▸ ")
                            yy = drawTextWithPrefix(page, "▸ ", mainText, bodyPaint, yy, suffix = null)
                            if (aiText != null) {
                                yy = drawText(page, "($aiText)", deadlinePaint, yy, indent = noteBulletWidth)
                            }

                            val result = checkPageBreak(document, page, yy, pageNumber)
                            page = result.first
                            yy = result.second
                        }
                        y = yy
                        y += LINE_HEIGHT
                    }
                }

                y += LINE_HEIGHT
            }

            document.finishPage(page)

            // ===== SAVE FILE =====
            val fileName = "LifeBalance_${System.currentTimeMillis()}.pdf"
            val dir = File(context.cacheDir, "pdf_export").apply { mkdirs() }
            val file = File(dir, fileName)

            FileOutputStream(file).use { document.writeTo(it) }
            document.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Result.success(PdfExportResult(uri, file))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Helpers ----------

    private fun createPage(
        document: PdfDocument,
        pageNumber: Int
    ): PdfDocument.Page {
        return document.startPage(
            PdfDocument.PageInfo.Builder(
                PAGE_WIDTH,
                PAGE_HEIGHT,
                pageNumber
            ).create()
        )
    }

    private fun checkPageBreak(
        document: PdfDocument,
        page: PdfDocument.Page,
        y: Float,
        nextPageNumber: Int
    ): Pair<PdfDocument.Page, Float> {
        return if (y > PAGE_HEIGHT - MARGIN - LINE_HEIGHT * 2) {
            document.finishPage(page)
            createPage(document, nextPageNumber) to MARGIN
        } else {
            page to y
        }
    }

    private fun drawTitle(
        page: PdfDocument.Page,
        text: String,
        y: Float
    ): Float {
        val x = PAGE_WIDTH / 2f - titlePaint.measureText(text) / 2f
        page.canvas.drawText(text, x, y, titlePaint)
        return y + LINE_HEIGHT * 1.5f
    }

    private fun drawSection(
        page: PdfDocument.Page,
        text: String,
        y: Float
    ): Float {
        val x = PAGE_WIDTH / 2f - sectionPaint.measureText(text) / 2f
        page.canvas.drawText(text, x, y, sectionPaint)
        return y + LINE_HEIGHT
    }

    /**
     * Draws prefix on first line, content with wrapping. Wrapped lines align under content start.
     * Optional suffix (e.g. "Перенесено из целей") is drawn in gray italic right after content.
     * @param indent extra left offset (e.g. for nested items under a folder).
     */
    private fun drawTextWithPrefix(
        page: PdfDocument.Page,
        prefix: String,
        content: String,
        paint: Paint,
        startY: Float,
        suffix: String? = null,
        indent: Float = 0f
    ): Float {
        val startX = MARGIN + indent
        val maxWidth = PAGE_WIDTH - MARGIN * 2 - indent
        val prefixWidth = paint.measureText(prefix)
        val contentMaxWidth = maxWidth - prefixWidth
        val lines = wrapText(content, paint, contentMaxWidth)
        var y = startY

        if (lines.isEmpty()) {
            page.canvas.drawText(prefix, startX, y, paint)
            if (suffix != null) {
                page.canvas.drawText(" $suffix", startX + prefixWidth, y, deadlinePaint)
            }
            return y + LINE_HEIGHT * 0.9f
        }

        if (suffix == null) {
            lines.forEachIndexed { index, line ->
                if (index == 0) {
                    page.canvas.drawText(prefix, startX, y, paint)
                }
                page.canvas.drawText(line, startX + prefixWidth, y, paint)
                y += LINE_HEIGHT * 0.9f
            }
            return y
        }

        val suffixText = " $suffix"
        lines.dropLast(1).forEachIndexed { index, line ->
            if (index == 0) {
                page.canvas.drawText(prefix, startX, y, paint)
            }
            page.canvas.drawText(line, startX + prefixWidth, y, paint)
            y += LINE_HEIGHT * 0.9f
        }

        val lastLine = lines.last()
        val lastLineWidth = paint.measureText(lastLine)
        val suffixWidth = deadlinePaint.measureText(suffixText)

        if (lines.size == 1) {
            page.canvas.drawText(prefix, startX, y, paint)
        }
        page.canvas.drawText(lastLine, startX + prefixWidth, y, paint)

        if (lastLineWidth + suffixWidth <= contentMaxWidth) {
            page.canvas.drawText(suffixText, startX + prefixWidth + lastLineWidth, y, deadlinePaint)
        } else {
            y += LINE_HEIGHT * 0.9f
            page.canvas.drawText(suffixText.trimStart(), startX + prefixWidth, y, deadlinePaint)
        }
        return y + LINE_HEIGHT * 0.9f
    }

    private fun drawText(
        page: PdfDocument.Page,
        text: String,
        paint: Paint,
        startY: Float,
        indent: Float = 0f
    ): Float {
        val maxWidth = PAGE_WIDTH - MARGIN * 2 - indent
        val lines = wrapText(text, paint, maxWidth)
        var y = startY

        lines.forEach {
            page.canvas.drawText(it, MARGIN + indent, y, paint)
            y += LINE_HEIGHT * 0.9f
        }
        return y
    }

    /**
     * Draws main text with wrapping, then appends the suffix (e.g. deadline) in gray italic.
     */
    private fun drawTextWithRedSuffix(
        page: PdfDocument.Page,
        mainText: String,
        redSuffix: String,
        startY: Float,
        indent: Float = 0f
    ): Float {
        val maxWidth = PAGE_WIDTH - MARGIN * 2 - indent
        val lines = wrapText(mainText, bodyPaint, maxWidth)
        var y = startY

        if (lines.isEmpty()) {
            page.canvas.drawText(redSuffix.trimStart(), MARGIN + indent, y, deadlinePaint)
            return y + LINE_HEIGHT * 0.9f
        }

        lines.dropLast(1).forEach {
            page.canvas.drawText(it, MARGIN + indent, y, bodyPaint)
            y += LINE_HEIGHT * 0.9f
        }

        val lastLine = lines.last()
        val lastLineWidth = bodyPaint.measureText(lastLine)
        val suffixWidth = deadlinePaint.measureText(redSuffix)

        page.canvas.drawText(lastLine, MARGIN + indent, y, bodyPaint)

        if (lastLineWidth + suffixWidth <= maxWidth) {
            page.canvas.drawText(redSuffix, MARGIN + indent + lastLineWidth, y, deadlinePaint)
        } else {
            y += LINE_HEIGHT * 0.9f
            page.canvas.drawText(redSuffix.trimStart(), MARGIN + indent, y, deadlinePaint)
        }
        return y + LINE_HEIGHT * 0.9f
    }

    private fun wrapText(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)

        val result = mutableListOf<String>()
        var remaining = text

        while (remaining.isNotEmpty()) {
            val count = paint.breakText(remaining, true, maxWidth, null)
            val line = remaining.take(count)
            val split = line.lastIndexOf(' ').takeIf { it > line.length / 2 } ?: count
            result.add(remaining.take(split).trim())
            remaining = remaining.drop(split).trimStart()
        }
        return result
    }
}
