package com.example.gptlightversion.code

import lombok.extern.slf4j.Slf4j
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.jline.utils.Log
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream

@Component
@Slf4j
class MyConverter(var writer: FileWriter) {


    fun convertDocxToTxt(file: File, outputFolder: File = File("${file.parentFile.path}\\convert")): Boolean {
        return try {
            val s = readDocx(file)
            val number = file.extractNumber()
            val newFile = File("${outputFolder.path}\\$number.txt")
            if (!newFile.exists())
                newFile.mkdirs()
            writer.write(s, newFile)
            true
        } catch (e: NoSuchFieldException) {
            Log.error("Файл ${file.path} не получилось конвертировать в .txt")
            false
        }
    }

    @Throws(NoSuchFieldException::class)
    fun readDocx(file: File?): String {
        try {
            if (file != null) {
                FileInputStream(file).use { fileInputStream ->

                    // открываем файл и считываем его содержимое в объект XWPFDocument
                    val docxFile = XWPFDocument(OPCPackage.open(fileInputStream))
                    val extractor = XWPFWordExtractor(docxFile)
                    return extractor.text
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        throw NoSuchFieldException()
    }
}