package ru.netology;

import java.util.List;
import java.util.Map;

public record Request(
        String method,
        String path,
        String protocolVerse,
        List<String> headers,
        String body) {


    public var getQueryParam(String name){

    }

    public var qetQueryParams(){

    }
}
