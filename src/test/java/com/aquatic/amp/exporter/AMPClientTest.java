// Copyright (c) 2019 Uber Technologies, Inc.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the 'Software'), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.aquatic.amp.exporter;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xerial.snappy.Snappy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AMPClientTest {
    private AMPClient client;

    @BeforeEach
    public void beforeEach() {
        SimpleHttpClient<String> httpClient = StubResponse::create;
        client = new AMPClient(Region.US_EAST_1, "abc123", () -> AwsBasicCredentials.create("akid", "skid"), httpClient);
    }

    @Test
    public void canWriteSamples() throws IOException, InterruptedException {
        var writeRequest = Prometheus.WriteRequest.newBuilder().addTimeseries(Prometheus.TimeSeries.newBuilder().build()).build();
        HttpResponse<String> response = client.writeSamples(writeRequest);
        HttpRequest request = response.request();
        assertEquals(URI.create("https://aps-workspaces.us-east-1.amazonaws.com/workspaces/abc123/api/v1/remote_write"), request.uri());
        assertEquals(Snappy.compress(writeRequest.toByteArray()).length, request.bodyPublisher().orElseThrow().contentLength());
        assertEquals(emptyList(), request.headers().allValues("Host"));
    }

    @Test
    public void canGetDataViaHttpApi() throws IOException, InterruptedException {
        var response = client.httpRequest("/api/v1/query", Map.of("query", "myLabel"));
        HttpRequest request = response.request();
        assertEquals(URI.create("https://aps-workspaces.us-east-1.amazonaws.com/workspaces/abc123/api/v1/query?query=myLabel"), request.uri());
        assertEquals(emptyList(), request.headers().allValues("Host"));
    }

    private static class StubResponse<T> implements HttpResponse<T> {
        private final int status;
        private final T body;
        private final HttpHeaders headers = HttpHeaders.of(Collections.emptyMap(), (k, s) -> true);

        HttpClient.Version version = HttpClient.Version.HTTP_1_1;
        URI uri;
        SSLSession sslSession;
        HttpResponse<T> previousResponse;
        HttpRequest request;

        public StubResponse(int status) {
            this(status, null);
        }

        public StubResponse(int status, T body) {
            this.status = status;
            this.body = body;
        }

        public int statusCode() {
            return status;
        }

        public HttpRequest request() {
            return request;
        }

        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.ofNullable(previousResponse);
        }

        public HttpHeaders headers() {
            return headers;
        }

        public T body() {
            return body;
        }

        public Optional<SSLSession> sslSession() {
            return Optional.ofNullable(sslSession);
        }

        public URI uri() {
            return uri;
        }

        public HttpClient.Version version() {
            return version;
        }

        public static <T> HttpResponse<T> create(HttpRequest httpRequest, BodyHandler<T> bodyHandler) {
            var response = new StubResponse<T>(200);
            response.request = httpRequest;
            return response;
        }
    }
}
