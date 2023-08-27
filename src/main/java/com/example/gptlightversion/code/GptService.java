package com.example.gptlightversion.code;

import com.example.gptlightversion.code.exceptions.ChatGptResponseException;
import com.example.gptlightversion.code.exceptions.EmptyFileException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.net.http.HttpClient.Version.HTTP_2;

@Component
@Slf4j
@RequiredArgsConstructor
public class GptService {

    private final FileWriter writer;
    @Value("${ai.key}")
    private String token;
    private final HttpClient client = HttpClient.newBuilder()
            .version(HTTP_2)
            .build();



    public void chatGpt(File folder, int start, int end, String request) {
        IntStream.range(start, ++end)
                .forEach(i -> {
                    String filePath = String.format("%s\\%s.txt", folder.getPath(), i);
                    File file = new File(filePath);
                    if (file.exists()) {
                        chatGpt(file, request);
                    } else {
                        log.error("Попытка отправить в chatGpt несущуствующий файл {} ", filePath);
                    }

                });
    }

    @SneakyThrows
    public void chatGpt(File file, String request) {
        int fileNumber = extractFileNumber(file.getPath());
        String text = getTextFrom(file);
        String response;
        try {
            response = chatGpt(text, request);
        } catch (Exception e) {
            log.error("{} FALSE {}", fileNumber, e.getMessage());
            return;
        }
        log.info("{} true", fileNumber);

        long count = response.lines().count();
        if (count < 10) {
            log.info("Файл {} небыл перезаписан, количество строк после нейросети стало {}, а было {}",
                    fileNumber,
                    count,
                    text.lines().count());
        } else {
            writer.write(response, file);
        }

    }


    private String chatGpt(String textFile, String request) throws IOException, InterruptedException, URISyntaxException {
        String partOfRequest;
        partOfRequest = textFile.replaceAll("\\n", "\\\\n")
                .replaceAll("}", "")
                .replaceAll("\\{", "")
                .replaceAll(":", "")
                .replaceAll("\"", "")
                .replaceAll(";", "");
        partOfRequest = partOfRequest.replaceAll("\"", "");
        String requestTemplate = "{\"model\": \"gpt-3.5-turbo\",\n" +
                "     \"messages\": [{\"role\": \"user\", \"content\": \"" + request + "\\n \\n" + partOfRequest + "\"}],\n" +
                "     \"temperature\": 0.7}";


        HttpRequest httpRequest = HttpRequest.newBuilder()
                .timeout(Duration.ofMinutes(10))
                .version(HTTP_2)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(requestTemplate))
                .uri(new URI("https://api.openai.com/v1/chat/completions"))

                .build();
        HttpResponse<String> res = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        String body = res.body();
        String response = extractContentNewApi(body);
        if (response == null) {
            throw new ChatGptResponseException("ChatGpt response\nbody: " + body);
        }
        return response;
    }

    private String getTextFrom(File file) {
        StringBuffer buffer = new StringBuffer();
        try(Stream<String> lines = Files.lines(Paths.get(file.getPath()), StandardCharsets.UTF_8)) {
            lines
                    .forEach(string -> buffer.append(string).append("\n"));
        } catch (IOException e) {
            log.error("Не удалось прогнать через chatGpt файл {}, он оказался пустой", file.getPath());
            throw new EmptyFileException();
        }

        return buffer.toString();
    }

    private int extractFileNumber(String path) {
        Pattern compile = Pattern.compile("\\\\(\\d+)\\.");
        Matcher match = compile.matcher(path);
        if (match.find()) {
            return Integer.parseInt(match.group(1));
        } else {
            log.error("Неудачная попытка получить номер файла {}", path);
            return -1;
        }
    }

    @SneakyThrows
    public static String extractContentNewApi(String str) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(str);
        JsonNode jsonNode1 = jsonNode.findValue("content");
        try {
            return jsonNode1.asText();
        } catch (Exception e) {
            return null;
        }

    }


}
