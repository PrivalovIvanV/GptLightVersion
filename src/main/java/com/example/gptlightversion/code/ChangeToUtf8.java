package com.example.gptlightversion.code;

import com.example.gptlightversion.code.exceptions.CreateBufferFileException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChangeToUtf8 {


    public void encodeFolder(String folder) {
        File currentFolder = new File(folder);
        List<File> list = Arrays.asList(currentFolder.listFiles());
        List<String> fileNames = list.stream()
                .map(File::getName)
                .collect(Collectors.toList());

        fileNames.forEach(name -> {
            File file = new File(currentFolder + name);
            encode(file);
        });
    }

    public void encodeFile(String fileName) {
        File file = new File(fileName);
        encode(file);
    }


    @SneakyThrows
    private void encode(File file) {
        if (!isReadable(file)) {
            String originalFileName = file.getPath();

            File fileInput = createBufferFilledFile(file);
            File fileOutput = new File(originalFileName);

            FileInputStream inputStream = new FileInputStream(fileInput);
            FileOutputStream outStream = new FileOutputStream(fileOutput);


            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            Charset w1251 = StandardCharsets.UTF_16LE;
            Charset utf8 = StandardCharsets.UTF_8;


            String string = new String(buffer, utf8);

            outStream.write(string.getBytes(w1251));
            outStream.close();
            fileInput.deleteOnExit();

            log.error("File on path {} is recoded", file.getPath());

        } else {
            log.error("File on path {} don`t needed to record", file.getPath());
        }
    }

    private boolean isReadable(File file) {
        try {
            String s = Files.readString(file.toPath());
            System.out.print(s.substring(1, 1));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private File createBufferFilledFile(File file) {
        Optional<String> folder = extractFolderPath(file);
        if (folder.isPresent()) {
            String s = file.getName().split("\\.")[0];
            String bufferFileName = s + "_1.txt";
            File newFile = new File(folder.get() + bufferFileName);
            file.renameTo(newFile);
            return newFile;
        }
        throw new CreateBufferFileException("Не удалось создать копию файла " + file.getPath());
    }

    private Optional<String> extractFolderPath(File file) {
        Pattern pattern = Pattern.compile("\\/.*\\/");
        Matcher matcher = pattern.matcher(file.getPath());

        if (matcher.find()) {
            String group = matcher.group();
            return Optional.of(group);
        } else {
            log.error("File path {} is incorrectly", file.getPath());
            return Optional.empty();
        }

    }

}
