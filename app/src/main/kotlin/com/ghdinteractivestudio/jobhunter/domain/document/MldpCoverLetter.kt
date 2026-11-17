package com.ghdinteractivestudio.jobhunter.domain.document

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exact 7-part Formal Business Cover Letter model matching MLDP standard
 * (http://www.missionlocaledp.org/emploi/lettre-de-motivation), with Place & Date positioned Top-Right:
 * 1. Top-Left Sender | 2. Top-Right Place & Date | 3. Middle-Right Recipient |
 * 4. Left Bold Subject & Salutation | 5. Indented Body Paragraphs | 6. Closing Formula | 7. Right Signature.
 */
data class MldpCoverLetterData(
    // 1. Vos noms et coordonnées (Haut-Gauche)
    val senderName: String,
    val senderAddress: String,
    val senderPhone: String,
    val senderEmail: String,

    // 2. Destinataire (Milieu-Droite, sous lieu et date)
    val recipientCompany: String,
    val recipientAttention: String,
    val recipientLocation: String,

    // 3. Lieu et Date (Haut-Droite)
    val placeAndDate: String,

    // 4. Objet & Salutation (Gauche)
    val subject: String,
    val salutation: String,

    // 5. Corps en paragraphes MLDP ciblés sur le poste
    val bodyParagraphs: List<String>,

    // 6. Formule de politesse
    val closingFormula: String,

    // 7. Signature (Bas-Droite)
    val signatureName: String
) {
    fun toPlainText(): String = buildString {
        // 1. Lieu et date en haut à droite + coordonnées expéditeur en haut à gauche
        appendLine(placeAndDate)
        appendLine()
        appendLine(senderName)
        if (senderAddress.isNotBlank()) appendLine(senderAddress)
        if (senderPhone.isNotBlank()) appendLine("Tél : $senderPhone")
        if (senderEmail.isNotBlank()) appendLine("Email : $senderEmail")
        appendLine()

        // 2. Coordonnées destinataire
        appendLine(recipientCompany)
        if (recipientAttention.isNotBlank()) appendLine(recipientAttention)
        if (recipientLocation.isNotBlank()) appendLine(recipientLocation)
        appendLine()

        // 3. Objet + formule d'appel
        appendLine(subject)
        appendLine()
        appendLine(salutation)
        appendLine()

        // 5. Corps MLDP avec alinéas
        bodyParagraphs.forEach { para ->
            appendLine("    $para")
            appendLine()
        }

        // 6. Formule de politesse
        appendLine("    $closingFormula")
        appendLine()

        // 7. Signature
        appendLine(signatureName)
    }.trim()

    companion object {
        fun formatCurrentPlaceAndDate(city: String, isFrench: Boolean): String {
            val cleanCity = city.substringBefore(",").trim().ifBlank { if (isFrench) "Algérie" else "Location" }
            return if (isFrench) {
                val sdf = SimpleDateFormat("d MMMM yyyy", Locale.FRENCH)
                "$cleanCity, le ${sdf.format(Date())}"
            } else {
                val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
                "$cleanCity, ${sdf.format(Date())}"
            }
        }
    }
}
