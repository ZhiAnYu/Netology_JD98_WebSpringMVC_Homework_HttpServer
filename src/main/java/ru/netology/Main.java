package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        //создаем новый объект класса server
        Server server = new Server (9999);

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
            var response = "Your message was received";
            try {
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/plain \r\n" +
                                "Content-Length: " + response.length() + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(response.getBytes());
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