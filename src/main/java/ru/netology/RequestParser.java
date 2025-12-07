package ru.netology;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class RequestParser {
    //оборачиваем в Optional чтобы не работать с null
    public static Optional<Request> parse (BufferedInputStream in, int limit) throws IOException {
        in.mark(limit);
        final var buffer = new byte[limit];
        final int read;
        try {
            read = in.read(buffer);
        } catch (IOException e) {
            return Optional.empty();
        }

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);

        // читаем request line - это массив байт из буффера от 0 до requestLineEnd
        final var requestLineBytes = Arrays.copyOf(buffer, requestLineEnd);
        final var requestLineString = new String(requestLineBytes);
        final var requestLineArray = requestLineString.split(" ");

        // final var requestLineArray = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");

        //проверка на правильность запроса из трех частей
        if (requestLineArray.length != 3) {
            return Optional.empty();
        }

        final var method = requestLineArray[0];
        final var fullPath = requestLineArray[1];
        //проверка на правильность пути
        if (!fullPath.startsWith("/")) {
            return Optional.empty();
        }

        final var cleanPath = fullPath.substring(0, fullPath.indexOf('?'));
        final var protocolVerse = requestLineArray[2];

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        //проверка на наличие заголовков
        if (headersEnd == -1) {
            return Optional.empty();
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));

        // для GET тела нет
        String body = null;
        if (!method.equals("GET")) {
            in.skip(headersDelimiter.length);
            // находим заголовок Content-Length, чтобы узнать количество байт тела,
            // и прочитать из буффера это количество
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);

                body = new String(bodyBytes);
                System.out.println(body);
            }
        }


        Request request = new Request(method, fullPath, protocolVerse, headers, body, params);
        return Optional.of(request);
    }


    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
