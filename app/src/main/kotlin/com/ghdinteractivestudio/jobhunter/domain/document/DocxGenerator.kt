package com.ghdinteractivestudio.jobhunter.domain.document

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Clean OpenXML Word (.docx) Generator :
 * Creates valid Microsoft Word (.docx) files without external heavyweight libraries
 * conforming to ISO/IEC 29500-1:2016 Open Packaging Conventions.
 */
object DocxGenerator {

    fun generateDocx(
        title: String,
        sections: List<Pair<String, List<String>>>,
        outputFile: File
    ) {
        val documentXml = buildDocumentXml(title, sections)
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
            // [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(contentTypesXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(rootRelsXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(documentXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
    }

    private fun buildDocumentXml(
        title: String,
        sections: List<Pair<String, List<String>>>
    ): String {
        val bodyBuilder = StringBuilder()

        // Title
        bodyBuilder.append("""
            <w:p>
                <w:pPr>
                    <w:jc w:val="center"/>
                </w:pPr>
                <w:r>
                    <w:rPr>
                        <w:b/>
                        <w:sz w:val="36"/>
                        <w:color w:val="111827"/>
                    </w:rPr>
                    <w:t>${escapeXml(title)}</w:t>
                </w:r>
            </w:p>
        """.trimIndent())

        for ((sectionTitle, paragraphs) in sections) {
            // Section heading
            if (sectionTitle.isNotBlank()) {
                bodyBuilder.append("""
                    <w:p>
                        <w:pPr>
                            <w:spacing w:before="240" w:after="80"/>
                        </w:pPr>
                        <w:r>
                            <w:rPr>
                                <w:b/>
                                <w:sz w:val="26"/>
                                <w:color w:val="059669"/>
                            </w:rPr>
                            <w:t>${escapeXml(sectionTitle)}</w:t>
                        </w:r>
                    </w:p>
                """.trimIndent())
            }

            // Paragraphs / bullet items
            for (p in paragraphs) {
                bodyBuilder.append("""
                    <w:p>
                        <w:pPr>
                            <w:spacing w:after="120" w:line="276" w:lineRule="auto"/>
                        </w:pPr>
                        <w:r>
                            <w:rPr>
                                <w:sz w:val="22"/>
                                <w:color w:val="242A38"/>
                            </w:rPr>
                            <w:t>${escapeXml(p)}</w:t>
                        </w:r>
                    </w:p>
                """.trimIndent())
            }
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:body>
        $bodyBuilder
        <w:sectPr>
            <w:pgSz w:w="11906" w:h="16838"/>
            <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
        </w:sectPr>
    </w:body>
</w:document>"""
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
