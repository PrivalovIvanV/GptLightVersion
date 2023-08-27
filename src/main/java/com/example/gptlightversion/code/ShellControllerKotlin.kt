package com.example.gptlightversion.code

import org.springframework.beans.factory.annotation.Value
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.io.File
import java.util.Comparator


@ShellComponent
class ShellControllerKotlin(
        val transcoder: ChangeToUtf8,
        val converter: MyConverter,
        val ai: GptService
) {

    @Value("\${ai.default-request}")
    private lateinit var defaultRequest : String
    @ShellMethod(key = ["transcode"], value = "Transcode files from UTF-16")
    fun transcode(@ShellOption("-F") pathToFolder: String,
                  @ShellOption("-f") pathToFile: String): String {

        if (notNone(pathToFolder))
            if (isCorrect(pathToFolder, isFolder = true))
                transcoder.encodeFolder(pathToFolder)
            else
                return ("Название папки ${pathToFolder} не корректно")
        else
            if (notNone(pathToFile)) {
                isCorrect(pathToFile, isFile = true)
                transcoder.encodeFile(pathToFile)
            } else
                return ("Файл ${pathToFile} не существует")

        return "success";
    }



    @ShellMethod(key = ["gpt"], value = "Processing file with chatGpt")
    fun chatGpt(@ShellOption("-f", "--file", defaultValue = "NONE") fileInput: String,
                @ShellOption("-F", "--folder", defaultValue = "NONE") folder: String,
                @ShellOption("-r", "--request", defaultValue = "NONE") query: String,
                @ShellOption("-s", defaultValue = 1.toString()) start: Int,
                @ShellOption("-e", defaultValue = 0.toString()) end: Int){

        val request = if (notNone(query)) query else defaultRequest

        if (isCorrect(fileInput, isFile = true)) {
            val file = File(fileInput)
            ai.chatGpt(file, request)
        }
        if (isCorrect(folder, isFolder = true)) {
            val file = File(folder)
            var stop = end;
            if (stop == 0)
                stop = file.listFiles().size

            ai.chatGpt(file, start, stop, request)
        }

    }

    @ShellMethod(key = ["convert"], value = "Convert file or folder to txt")
    fun convertFileToTxt(@ShellOption("-f", "--file", defaultValue = "NONE") fileInput: String,
                         @ShellOption("-F", "--folder", defaultValue = "NONE") folder: String,
                         @ShellOption("-o", "--output-folder", defaultValue = "NONE") outputFolder: String){
        val output = if (notNone(outputFolder)) outputFolder else ""
        if(notNone(fileInput)){
            converter.convertDocxToTxt(file = File(fileInput))
        }

        if (notNone(folder)){
            val file = File(folder)
            file.listFiles()?.forEach {
                if (output.isNotBlank()) {
                    converter.convertDocxToTxt(file = it, File(output))
                } else {
                    converter.convertDocxToTxt(file = it)
                }
            }
        }

    }

    @ShellMethod(key = ["magic"], value = "Convert file or folder to txt")
    fun magic(@ShellOption("-F", "--folder", defaultValue = "NONE") folder: String){
        if (notNone(folder)){
            val folderForGpt = File("${folder}/convert")
            val file = File(folder)
            file.listFiles()?.forEach {
                converter.convertDocxToTxt(file = it, folderForGpt)
            }
            folderForGpt.listFiles().toMutableList()
                .stream()
                .sorted { o1, o2 ->
                    return@sorted if (o1.extractNumber() > o2.extractNumber()) 1 else -1
                }
                .forEach {
                ai.chatGpt(it, defaultRequest)
            }
            transcoder.encodeFolder(folderForGpt.path)
            //todo: дописать работу с БД
        }
    }




    private fun notNone(vararg fields: String) = fields.all { s -> !s.contains("none", ignoreCase = true) }

    private fun isCorrect(string: String,
                          isFolder: Boolean = false,
                          isFile: Boolean = false): Boolean {
        val file = File(string)
        val notNone = !string.contains("none", ignoreCase = true)
        var isConsistent = false
        if (isFolder)
            isConsistent = !file.name.contains(".")
        if (isFile)
            isConsistent = file.name.contains(".") && file.isFile
        return notNone && isConsistent;
    }


}