package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    //обязательные поля
    private final int PORT;
    private final List<String> VALID_PATH;
    private final int THREAD_AMOUNT = 64;
    final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_AMOUNT);
    //мапа для хранения обработчиков
    //Ключ первого уровня — HTTP-метод, второго — путь.
    private final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();


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

            var method = parts[0];
            var path = parts[1];
            var protocolVerse = parts[2];

            Request request = new Request(method, path, protocolVerse, null, null);

//            //проверка на наличие пути в запросе в списке разрешенных путей
//            if (!VALID_PATH.contains(path)) {
//                sendBadResponse(out, "404 Not found");
//                return;
//            }

//            final var filePath = Path.of(".", "public", path);
//            if (!Files.exists(filePath)) {
//                sendBadResponse(out, "404 Not Found");
//                //    System.out.println(filePath + " doesn't exist");
//                return;
//            }


//            final var mimeType = Files.probeContentType(filePath);
//            final var length = Files.size(filePath);

//            //пишем ответ сервера на запрос - отправляем запрошенный файл
//            sendResponse(out, mimeType, length, filePath);

//      Разъяснение ИИ:
//            Что делает getOrDefault?
//      Если в handlers есть запись с ключом "GET" → вернёт внутреннюю мапу вида Map<String, Handler>,
//      где ключи — пути.
//      Если нет такого метода (например, пришёл "PATCH", а его не регистрировали) → вернёт пустую,
//      неизменяемую мапу: Collections.emptyMap().
//💡       Collections.emptyMap() — это безопасная заглушка:
//      она не null,
//      у неё можно вызвать .get(...),
//      она всегда возвращает null на любой ключ,
//      она неизменяема, поэтому нельзя случайно что-то в неё записать.
//            Теперь вызываем .get(path) на результате предыдущего шага:
//      Случай A: метод "GET" существует → получили внутреннюю мапу → ищем ключ "/messages".
//           Если есть → получаем Handler.
//           Если нет → получаем null.
//      Случай B: метод "GET" не существует → получили Collections.emptyMap() → вызываем .get("/messages")
//      → всегда возвращает null.
//           Итог: в любом случае, если хендлер не найден — handler == null.

            Handler handler = handlers.getOrDefault(method, Collections.emptyMap())
                    .get(path);
            if (handler == null) {
                sendBadResponse(out, "404 Not found");
                return;
            }
            try {
                handler.handle(request, out);

            } catch (Exception ex) {
                //если ошибка будет в логике у пользователя, то делаем перехват
                sendBadResponse(out, "500 Internet server error");
            }

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

//    private void sendResponse(BufferedOutputStream out, String mimeType, long length, Path filePath) {
//        try {
//            out.write((
//                    "HTTP/1.1 200 OK\r\n" +
//                            "Content-Type: " + mimeType + "\r\n" +
//                            "Content-Length: " + length + "\r\n" +
//                            "Connection: close\r\n" +
//                            "\r\n"
//            ).getBytes());
//            Files.copy(filePath, out);
//            out.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    //метод для добавления обработчика
    public void addHandler(String method, String path, Handler handler) {
//        Сервер смотрит: есть ли в handlers ключ method (например, "GET")?
//        Если ДА → возвращает уже существующую внутреннюю мапу.
//        Если НЕТ → вызывает лямбду k -> new ConcurrentHashMap<>(),
//        создаёт новую мапу, автоматически кладёт её в handlers под ключом method,
//        и возвращает эту новую мапу.
//            У полученной (существующей или новой) внутренней мапы вызываем .put(path, handler).
//        То есть:
//        Если для "GET" уже была мапа — добавляем туда "/messages" → handler.
//        Если не было — создаём мапу для "GET", кладём туда "/messages" → handler.
        handlers.computeIfAbsent(method, k -> new ConcurrentHashMap<>()).put(path, handler);
    }
}
