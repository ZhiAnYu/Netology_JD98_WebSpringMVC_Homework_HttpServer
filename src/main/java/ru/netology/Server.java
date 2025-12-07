package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    //обязательные поля
    private final int PORT;
    private final List<String> ALLOWED_METHODS;
    private final int THREAD_AMOUNT = 64;
    private final int LIMIT = 4096;
    final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_AMOUNT);
    private final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();


    public Server(int port, List<String> allowedMethods) {
        PORT = port;
        ALLOWED_METHODS = allowedMethods;
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
             final var in = new BufferedInputStream(socket.getInputStream());
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            //проверка на допустимость вызываемого метода
            if (!ALLOWED_METHODS.contains(method)) {
                sendNotAllowedError(out);
                return;
            }
            System.out.println(method);
            System.out.println(fullPath);
            System.out.println(headers);



            Request request = new Request(method, fullPath, protocolVerse, headers, body);

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
//            Теперь вызываем .get(fullPath) на результате предыдущего шага:
//      Случай A: метод "GET" существует → получили внутреннюю мапу → ищем ключ "/messages".
//           Если есть → получаем Handler.
//           Если нет → получаем null.
//      Случай B: метод "GET" не существует → получили Collections.emptyMap() → вызываем .get("/messages")
//      → всегда возвращает null.
//           Итог: в любом случае, если хендлер не найден — handler == null.

            Handler handler = handlers.getOrDefault(method, Collections.emptyMap())
                    .get(cleanPath);
            if (handler == null) {
                sendNotFoundError(out);
                return;
            }
            try {
                handler.handle(request, out);
            } catch (Exception ex) {
                //если ошибка будет в логике у пользователя, то делаем перехват
                sendServerError(out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
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
    //обертки для sendResponse - для очевидности ответа (исключаем magicNumbers) / вебинар от 5.12.25
    private void sendBadRequestError (BufferedOutputStream out){
        sendResponseBodyless(out, "400 Bad request");
    }
    private void sendNotFoundError(BufferedOutputStream out) {
        sendResponseBodyless(out, "404 Not found");
    }
    private void sendServerError (BufferedOutputStream out) {
        sendResponseBodyless(out, "500 Internet server error");
    }
    private void sendNotAllowedError (BufferedOutputStream out) {sendResponseBodyless(out, "405 Not allowed");}
    private void sendHeadersAbsent (BufferedOutputStream out) {sendResponseBodyless(out, "422 Unprocessed Content");
    }


    void sendResponseBodyless(BufferedOutputStream bufferedOutputStream, String status) {
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

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}
