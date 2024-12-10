package com.aquatic.amp.exporter;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

interface SimpleHttpClient<T> {
    HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException;
}
