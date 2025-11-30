package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    //обязательные поля
    private final int PORT;
    private final List<String> VALID_PATH;
    private final int THREAD_AMOUNT = 64;
    final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_AMOUNT);

    public Server(int port, List<String> validPath) {
        PORT = port;
        VALID_PATH = validPath;
    //    System.out.println("Server created");
    }

    //создаем метод только для старта сервера и добавления сокетов(клиентов) отдельным потоком в пул
    public void start() {
        try (final var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                final var socket = serverSocket.accept();
                threadPool.submit(() -> handleConnection(socket));
    //            System.out.println("Socket accept");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();

        }
    }

    //создаем метод для обработки конкретного подключением
    public void handleConnection(Socket socket) {
        try (socket;
             final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");
    //        System.out.println(requestLine);
            //проверка на три части
            if (parts.length != 3) {
                sendBadResponse(out, "400 Bad request");
                return;
            }

            var path = parts[1];
            //проверка на наличие пути в запросе в списке разрешенных путей
            if (!VALID_PATH.contains(path)) {
                sendBadResponse(out, "404 Not found");
                return;
            }

            final var filePath = Path.of(".", "public", path);
            if (!Files.exists(filePath)) {
                sendBadResponse(out, "404 Not Found");
            //    System.out.println(filePath + " doesn't exist");
                return;
            }


            final var mimeType = Files.probeContentType(filePath);
            final var length = Files.size(filePath);

            //пишем ответ сервера на запрос - отправляем запрошенный файл
            sendResponse(out, mimeType, length, filePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendBadResponse(BufferedOutputStream bufferedOutputStream, String status) {
        try {
            bufferedOutputStream.write((
                    "HTTP/1.1 " + status + "\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            bufferedOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendResponse(BufferedOutputStream out, String mimeType, long length, Path filePath) {
        try {
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
