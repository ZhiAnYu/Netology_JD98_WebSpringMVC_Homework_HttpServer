package ru.netology;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

public class RequestParser {

    //оборачиваем в Optional чтобы не работать с null
    public static Optional<Request> parse(BufferedReader in) {
        final String requestLine;
        try {
            requestLine = in.readLine();
        } catch (IOException e) {
            return Optional.empty();
        }
        final var parts = requestLine.split(" ");

        if (parts.length != 3) {
            return Optional.empty();
        }

        var method = parts[0];
        var path = parts[1];
        var protocolVerse = parts[2];

        Request request = new Request(method, path, protocolVerse, null, null);
        return Optional.of(request);
    }
}
