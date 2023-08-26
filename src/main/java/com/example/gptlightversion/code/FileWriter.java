package com.example.gptlightversion.code;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

@Component
@Slf4j
public class FileWriter {

    @SneakyThrows
    public void println(String value, File outputFile){
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }
        Files.write(outputFile.toPath(), value.getBytes(), StandardOpenOption.APPEND);

    }

    public void write(String value, File outputFile){
        if (!outputFile.exists()) {
            try {
                outputFile.createNewFile();
            } catch (IOException e) {
                log.error("Не удалось создать файл {}", outputFile.getPath());
            }
        }
        try {
            Files.write(outputFile.toPath(), value.getBytes(), StandardOpenOption.WRITE);
        } catch (IOException e) {
            log.error("Не удалось перезаписать в файл {}", outputFile.getPath());            }


    }

}
