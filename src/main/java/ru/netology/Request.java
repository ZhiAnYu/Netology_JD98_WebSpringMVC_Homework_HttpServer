package ru.netology;

public record Request(String method, String path, String protocolVerse, String[] headersRequest, byte[] bodyRequest) {

}
