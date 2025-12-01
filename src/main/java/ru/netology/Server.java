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
    //private final List<String> VALID_PATH;
    private final List<String> ALLOWED_METHODS;
    private final int THREAD_AMOUNT = 64;
    private final int LIMIT = 4096;
    final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_AMOUNT);
    //мапа для хранения обработчиков
    //Ключ первого уровня — HTTP-метод, второго — путь.
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

            // отмечаем количество байт в лимите - ставим метку на буффере входящего потока

            in.mark(LIMIT);
            final var buffer = new byte[LIMIT];
            final var read = in.read(buffer);

            // ищем request line
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                sendBadResponse(out, "400 Bad request"); //делиметр не попался в массиве байт в пределах лимита
                return;
            }

            // читаем request line - это массив байт из буффера от 0 до requestLineEnd
            final var requestLineBytes = Arrays.copyOf(buffer, requestLineEnd);
            final var requestLineString = new String(requestLineBytes);
            final var requestLineArray = requestLineString.split(" ");
            // final var requestLineArray = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");

            //проверка на правильность запроса из трех частей
            if (requestLineArray.length != 3) {
                sendBadResponse(out, "400 Bad request");
                return;
            }

            final var method = requestLineArray[0];
            //проверка на допустимость вызываемого метода
            if (!ALLOWED_METHODS.contains(method)) {
                sendBadResponse(out,"405 Not allowed");
                return;
            }
            System.out.println(method);

            final var fullPath = requestLineArray[1];
            //проверка на правильность пути
            if (!fullPath.startsWith("/")) {
                sendBadResponse(out, "400 Bad request");
                return;
            }
            System.out.println(fullPath);

            final var cleanPath = fullPath.substring(0, fullPath.indexOf('?'));
            System.out.println(cleanPath);


            final var protocolVerse = requestLineArray[2];

            // ищем заголовки
            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                sendBadResponse(out, "422 Unprocessed Content");
                return;
            }

            // отматываем на начало буфера
            in.reset();
            // пропускаем requestLine
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
            System.out.println(headers);

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

            Request request = new Request(method, fullPath, protocolVerse, headers, body);

//            //проверка на наличие пути в запросе в списке разрешенных путей
//            if (!VALID_PATH.contains(fullPath)) {
//                sendBadResponse(out, "404 Not found");
//                return;
//            }

//            final var filePath = Path.of(".", "public", fullPath);
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

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}
