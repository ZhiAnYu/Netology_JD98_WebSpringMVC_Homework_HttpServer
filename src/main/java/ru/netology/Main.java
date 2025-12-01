package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static final String GET = "GET";
    public static final String POST = "POST";

    public static void main(String[] args) {
        //создаем список допустимых файлов
        //final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
        //создаем новый объект класса server

        final var allowedMethods = List.of(GET, POST);

        Server server = new Server(9999, allowedMethods);

        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/hello", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                String response = "Hello, User!";
                try {
                    responseStream.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/plain\r\n" +
                                    "Content-Length: " + response.length() + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    responseStream.write(response.getBytes());
                    responseStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        server.addHandler("GET", "/message", (Request request, BufferedOutputStream out) -> {
            var responce = "Your message was received";
            try {
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/plain \r\n" +
                                "Content-Length: " + responce.length() + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(responce.getBytes());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        server.addHandler("GET", "/image", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) throws IOException {
                var filePath = Path.of(".", "public", "/spring.png");
                var length = Files.size(filePath);
                try {
                    responseStream.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: image/png\r\n" +
                                    "Content-Length: " + length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    Files.copy(filePath, responseStream);
                    responseStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        server.start();
    }
}