package com.ghdinteractivestudio.jobhunter.domain.document

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Clean OpenXML Word (.docx) Generator :
 * Creates valid Microsoft Word (.docx) files without external heavyweight libraries
 * conforming to ISO/IEC 29500-1:2016 Open Packaging Conventions,
 * including exact MLDP 7-part cover letter alignment and paragraph indentation.
 */
object DocxGenerator {

    fun generateDocx(
        title: String,
        sections: List<Pair<String, List<String>>>,
        outputFile: File
    ) {
        val documentXml = buildCvDocumentXml(title, sections)
        writeDocxArchive(documentXml, outputFile)
    }

    /**
     * Generates a formal Word (.docx) Cover Letter strictly formatted after the MLDP standard
     * with Place & Date positioned Top-Right:
     * (Top-Left Sender | Top-Right Place & Date -> Middle-Right Recipient ->
     * Left Bold Subject & Salutation -> Indented Body Paragraphs -> Politeness Formula -> Right Signature).
     */
    fun generateMldpCoverLetterDocx(
        letter: MldpCoverLetterData,
        outputFile: File
    ) {
        val body = StringBuilder()

        // 1 & 2. Tableau sans bordure : Haut-Gauche (Expéditeur) | Haut-Droite (Lieu et Date)
        val senderCellParagraphs = buildString {
            append(paragraphXml(letter.senderName, bold = true, sizeHalfPt = 22, align = "left", spaceAfter = 40))
            listOfNotNull(
                letter.senderAddress.ifBlank { null },
                letter.senderPhone.ifBlank { null },
                letter.senderEmail.ifBlank { null }
            ).forEach { line ->
                append(paragraphXml(line, bold = false, sizeHalfPt = 20, color = "4B5563", align = "left", spaceAfter = 40))
            }
        }
        val dateCellParagraphs = paragraphXml(letter.placeAndDate, bold = false, sizeHalfPt = 22, align = "right", spaceAfter = 40)

        body.append(
            """
            <w:tbl>
                <w:tblPr>
                    <w:tblW w:w="9026" w:type="dxa"/>
                    <w:tblBorders>
                        <w:top w:val="none" w:sz="0" w:space="0" w:color="auto"/>
                        <w:left w:val="none" w:sz="0" w:space="0" w:color="auto"/>
                        <w:bottom w:val="none" w:sz="0" w:space="0" w:color="auto"/>
                        <w:right w:val="none" w:sz="0" w:space="0" w:color="auto"/>
                        <w:insideH w:val="none" w:sz="0" w:space="0" w:color="auto"/>
                        <w:insideV w:val="none" w:sz="0" w:space="0" w:color="auto"/>
                    </w:tblBorders>
                </w:tblPr>
                <w:tblGrid>
                    <w:gridCol w:w="4513"/>
                    <w:gridCol w:w="4513"/>
                </w:tblGrid>
                <w:tr>
                    <w:tc>
                        <w:tcPr><w:tcW w:w="4513" w:type="dxa"/></w:tcPr>
                        $senderCellParagraphs
                    </w:tc>
                    <w:tc>
                        <w:tcPr><w:tcW w:w="4513" w:type="dxa"/></w:tcPr>
                        $dateCellParagraphs
                    </w:tc>
                </w:tr>
            </w:tbl>
            """.trimIndent()
        )

        // 3. Destinataire (Milieu-Droite sous la date / expéditeur)
        body.append(paragraphXml(letter.recipientCompany, bold = true, sizeHalfPt = 22, align = "left", leftIndent = 5000, spaceBefore = 200, spaceAfter = 40))
        if (letter.recipientAttention.isNotBlank()) {
            body.append(paragraphXml(letter.recipientAttention, bold = false, sizeHalfPt = 20, color = "4B5563", align = "left", leftIndent = 5000, spaceAfter = 40))
        }
        if (letter.recipientLocation.isNotBlank()) {
            body.append(paragraphXml(letter.recipientLocation, bold = false, sizeHalfPt = 20, color = "4B5563", align = "left", leftIndent = 5000, spaceAfter = 220))
        } else {
            body.append(paragraphXml("", sizeHalfPt = 20, spaceAfter = 120))
        }

        // 4. Objet & Formule d'appel (Gauche)
        body.append(paragraphXml(letter.subject, bold = true, sizeHalfPt = 22, align = "left", spaceBefore = 80, spaceAfter = 200))
        body.append(paragraphXml(letter.salutation, bold = false, sizeHalfPt = 22, align = "left", spaceAfter = 180))

        // 5. Corps de texte MLDP (Alinéas avec retrait de première ligne 720 twips = ~1.27cm)
        letter.bodyParagraphs.filter { it.isNotBlank() }.forEach { para ->
            body.append(
                paragraphXml(
                    text = para.trim(),
                    bold = false,
                    sizeHalfPt = 22,
                    align = "both",
                    firstLineIndent = 720,
                    spaceAfter = 160
                )
            )
        }

        // 6. Formule de politesse
        if (letter.closingFormula.isNotBlank()) {
            body.append(
                paragraphXml(
                    text = letter.closingFormula.trim(),
                    bold = false,
                    sizeHalfPt = 22,
                    align = "both",
                    firstLineIndent = 720,
                    spaceAfter = 280
                )
            )
        }

        // 7. Signature (Bas-Droite)
        body.append(paragraphXml(letter.signatureName, bold = true, sizeHalfPt = 22, align = "right", spaceBefore = 160, spaceAfter = 80))

        val documentXml = wrapWordDocumentXml(body.toString())
        writeDocxArchive(documentXml, outputFile)
    }

    private fun buildCvDocumentXml(
        title: String,
        sections: List<Pair<String, List<String>>>
    ): String {
        val bodyBuilder = StringBuilder()

        // Title
        bodyBuilder.append(paragraphXml(title, bold = true, sizeHalfPt = 34, color = "111827", align = "center", spaceAfter = 180))

        for ((sectionTitle, paragraphs) in sections) {
            if (sectionTitle.isNotBlank()) {
                bodyBuilder.append(
                    paragraphXml(
                        text = sectionTitle.uppercase(),
                        bold = true,
                        sizeHalfPt = 26,
                        color = "059669",
                        align = "left",
                        spaceBefore = 220,
                        spaceAfter = 80
                    )
                )
            }

            // Split on real newlines so Word preserves every line break
            for (p in paragraphs) {
                p.split(Regex("\\r?\\n")).forEach { subLine ->
                    val cleaned = subLine.trim()
                    if (cleaned.isNotBlank()) {
                        bodyBuilder.append(
                            paragraphXml(
                                text = cleaned,
                                bold = false,
                                sizeHalfPt = 21,
                                color = "242A38",
                                align = "left",
                                spaceAfter = 100
                            )
                        )
                    }
                }
            }
        }

        return wrapWordDocumentXml(bodyBuilder.toString())
    }

    private fun paragraphXml(
        text: String,
        bold: Boolean = false,
        sizeHalfPt: Int = 22,
        color: String = "1F2937",
        align: String = "left",
        leftIndent: Int = 0,
        firstLineIndent: Int = 0,
        spaceBefore: Int = 0,
        spaceAfter: Int = 120
    ): String {
        val indAttrs = buildString {
            if (leftIndent > 0) append(""" w:left="$leftIndent"""")
            if (firstLineIndent > 0) append(""" w:firstLine="$firstLineIndent"""")
        }
        val indTag = if (indAttrs.isNotEmpty()) "<w:ind$indAttrs/>" else ""
        val boldTag = if (bold) "<w:b/>" else ""
        return """
            <w:p>
                <w:pPr>
                    <w:jc w:val="$align"/>
                    $indTag
                    <w:spacing w:before="$spaceBefore" w:after="$spaceAfter" w:line="276" w:lineRule="auto"/>
                </w:pPr>
                <w:r>
                    <w:rPr>
                        $boldTag
                        <w:sz w:val="$sizeHalfPt"/>
                        <w:color w:val="$color"/>
                    </w:rPr>
                    <w:t xml:space="preserve">${escapeXml(text)}</w:t>
                </w:r>
            </w:p>
        """.trimIndent()
    }

    private fun wrapWordDocumentXml(bodyContent: String): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:body>
        $bodyContent
        <w:sectPr>
            <w:pgSz w:w="11906" w:h="16838"/>
            <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
        </w:sectPr>
    </w:body>
</w:document>"""

    private fun writeDocxArchive(documentXml: String, outputFile: File) {
        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(contentTypesXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(rootRelsXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(documentXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
