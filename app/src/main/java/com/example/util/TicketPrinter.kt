package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.example.data.local.ParkingSessionEntity
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TicketPrinter {

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun printTicket(
        context: Context,
        session: ParkingSessionEntity,
        isEnglish: Boolean,
        onError: (String) -> Unit = {}
    ) {
        val activity = context.findActivity()
        if (activity == null) {
            onError(
                if (isEnglish) "Unable to open the print dialog from this screen."
                else "Impossible d'ouvrir l'impression depuis cet écran."
            )
            return
        }

        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            onError(
                if (isEnglish) "Print service unavailable on this device."
                else "Service d'impression indisponible sur cet appareil."
            )
            return
        }

        val jobName = "PARKO_Ticket_${session.referenceCode}"
        val adapter = ParkoTicketPrintAdapter(activity, session, isEnglish)
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, adapter, attributes)
    }

    private class ParkoTicketPrintAdapter(
        private val activity: Activity,
        private val session: ParkingSessionEntity,
        private val isEnglish: Boolean
    ) : PrintDocumentAdapter() {

        private var pdfDocument: PrintedPdfDocument? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            pdfDocument = PrintedPdfDocument(activity, newAttributes)
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("PARKO_${session.referenceCode}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val doc = pdfDocument ?: return
            try {
                val page = doc.startPage(0)
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    doc.close()
                    pdfDocument = null
                    return
                }

                drawTicketPage(page.canvas, page.info.pageWidth.toFloat(), page.info.pageHeight.toFloat())
                doc.finishPage(page)

                FileOutputStream(destination.fileDescriptor).use { out ->
                    doc.writeTo(out)
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                doc.close()
                pdfDocument = null
            }
        }

        private fun drawTicketPage(canvas: android.graphics.Canvas, width: Float, height: Float) {
            canvas.drawColor(Color.WHITE)

            val centerX = width / 2f
            val margin = 20f
            var y = 30f

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(14, 84, 222)
                textSize = 20f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.LEFT
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 16f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1f
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
            }
            val qrPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }

            // Header
            canvas.drawText("PARKO", centerX, y, titlePaint)
            y += 16f
            canvas.drawText(session.parkingName, centerX, y, subHeaderPaint)
            y += 10f
            canvas.drawLine(margin, y, width - margin, y, dashPaint)
            y += 14f

            // QR Code
            val matrix = QrCodeGenerator.encode(session.qrToken)
            val qrSize = (width - margin * 2).coerceAtMost(130f)
            val qrLeft = centerX - qrSize / 2f
            val cellSize = qrSize / matrix.size.toFloat()
            for (row in matrix.indices) {
                for (col in matrix[row].indices) {
                    if (matrix[row][col]) {
                        canvas.drawRect(
                            qrLeft + col * cellSize,
                            y + row * cellSize,
                            qrLeft + (col + 1) * cellSize + 0.3f,
                            y + (row + 1) * cellSize + 0.3f,
                            qrPaint
                        )
                    }
                }
            }
            y += qrSize + 18f

            // Claim Code
            canvas.drawText(
                if (isEnglish) "DIGITAL RECEIPT CODE" else "CODE DE RÉCEPTION NUMÉRIQUE",
                centerX,
                y,
                Paint(labelPaint).apply { textAlign = Paint.Align.CENTER; textSize = 8.5f }
            )
            y += 16f
            canvas.drawText(session.referenceCode, centerX, y, codePaint)
            y += 12f
            canvas.drawLine(margin, y, width - margin, y, dashPaint)
            y += 16f

            // Details
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", if (isEnglish) Locale.ENGLISH else Locale.FRANCE)
            val rows = listOf(
                (if (isEnglish) "Vehicle" else "Véhicule") to session.vehicleLabel,
                (if (isEnglish) "Plate" else "Plaque") to session.plateNumber,
                (if (isEnglish) "Color" else "Couleur") to session.vehicleColor,
                (if (isEnglish) "Owner" else "Propriétaire") to session.ownerName,
                (if (isEnglish) "Entry" else "Entrée") to sdf.format(Date(session.entryTime)),
                (if (isEnglish) "Attendant" else "Gardien") to session.attendantName,
                (if (isEnglish) "Parking fee" else "Tarif") to "${session.feeFcfa} FCFA"
            )
            rows.forEach { (k, v) ->
                if (y < height - 40f) {
                    canvas.drawText(k, margin, y, labelPaint)
                    canvas.drawText(v, width - margin, y, valuePaint)
                    y += 15f
                }
            }

            y += 6f
            canvas.drawLine(margin, y, width - margin, y, dashPaint)
            y += 14f

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                if (isEnglish) "Present this ticket or claim code in PARKO at exit."
                else "Présentez ce ticket ou saisissez le code dans PARKO à la sortie.",
                centerX,
                y,
                footerPaint
            )
        }
    }
}
