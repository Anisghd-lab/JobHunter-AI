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
    val signatureName: String,

    val isFrench: Boolean = true
) {
    fun toPlainText(): String = buildString {
        // 1. Lieu et date en haut à droite + coordonnées expéditeur en haut à gauche
        appendLine(placeAndDate)
        appendLine()
        appendLine(senderName)
        if (senderAddress.isNotBlank()) appendLine(senderAddress)
        if (senderPhone.isNotBlank()) appendLine(if (isFrench) "Tél : $senderPhone" else "Tel: $senderPhone")
        if (senderEmail.isNotBlank()) appendLine("Email: $senderEmail")
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
        private val KNOWN_CITIES = listOf(
            "Oum El Bouaghi", "Sidi Bel Abbès", "Bordj Bou Arréridj", "Tizi Ouzou",
            "Aïn Témouchent", "Ain Temouchent", "Aïn Defla", "Ain Defla",
            "Souk Ahras", "Hassi Messaoud", "El Oued", "El Tarf", "El Bayadh",
            "In Salah", "Abu Dhabi", "Ras Al Khaimah", "Kuwait City",
            "Constantine", "Mostaganem", "Tébessa", "Tlemcen", "Khenchela",
            "Boumerdès", "Ghardaïa", "Annaba", "Béjaïa", "Bejaia", "Biskra",
            "Ouargla", "Laghouat", "Skikda", "Guelma", "Tiaret", "Relizane",
            "Mascara", "Tindouf", "Adrar", "Béchar", "Batna", "Sétif", "Setif",
            "Blida", "Chlef", "Médéa", "Medea", "Bouira", "Tipaza", "Mila",
            "Jijel", "M'Sila", "Msila", "Saïda", "Saida", "Alger", "Algiers", "Oran",
            "Riyadh", "Jeddah", "Dammam", "Khobar", "Dubai", "Sharjah",
            "Doha", "Al Rayyan", "Muscat", "Manama", "Paris", "Lyon"
        )

        private val NON_CITY_COUNTRIES = setOf(
            "algerie", "algérie", "algeria", "dz", "france", "maroc", "morocco",
            "tunisie", "tunisia", "uae", "ksa", "qatar", "kuwait", "oman", "bahrain"
        )

        fun extractCityName(address: String, isFrench: Boolean): String {
            val raw = address.trim()
            if (raw.isBlank()) return if (isFrench) "Algérie" else "Algeria"

            val lowerRaw = raw.lowercase()
            KNOWN_CITIES.firstOrNull { lowerRaw.contains(it.lowercase()) }?.let { return it }

            val cleaned = raw
                .replace(Regex("""\(\s*\d{4,6}\s*\)"""), " ")
                .replace(Regex("""\b\d{4,6}\b"""), " ")
                .replace(Regex("""(?i)\b(n°|no\.?|numéro)\s*\w+"""), " ")

            val segments = cleaned.split(",", "|", "-", "\n")
                .map { it.trim() }
                .filter { seg ->
                    val segLow = seg.lowercase()
                    seg.isNotBlank() &&
                        segLow !in NON_CITY_COUNTRIES &&
                        !Regex("""^(cité|cite|rue|avenue|av\.?|boulevard|bd|lot|lotissement|résidence|residence|bâtiment|batiment|bt\.?|quartier|bp|p\.o\.)\b""", RegexOption.IGNORE_CASE).containsMatchIn(segLow)
                }

            val best = (segments.lastOrNull() ?: cleaned)
                .replace(Regex("""(?i)\b(algérie|algerie|algeria|france|saudi arabia|uae)\b"""), "")
                .replace(Regex("""\s+"""), " ")
                .trim()

            if (best.isBlank()) return if (isFrench) "Algérie" else "Algeria"
            return best.split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { w ->
                    w.lowercase().replaceFirstChar { c -> c.titlecase(Locale.getDefault()) }
                }
        }

        fun formatCurrentPlaceAndDate(city: String, isFrench: Boolean): String {
            val cleanCity = extractCityName(city, isFrench)
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
