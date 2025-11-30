package ru.netology;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        //создаем список допустимых файлов
        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
        //создаем новый объект класса server
        Server server = new Server (9999, validPaths);
        server.start();

    }
}