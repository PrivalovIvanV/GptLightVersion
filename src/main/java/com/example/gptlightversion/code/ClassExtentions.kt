package com.example.gptlightversion.code

import java.io.File

fun File.extractNumber() : Int{
    try {
        val int = this.name.split(".")[0].toInt()
        return int
    }catch (e : NumberFormatException){
        throw e
    }
}