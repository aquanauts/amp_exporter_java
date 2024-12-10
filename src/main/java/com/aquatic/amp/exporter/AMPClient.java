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

import org.xerial.snappy.Snappy;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.util.Collections;
import java.util.Map;

import static java.time.ZoneOffset.UTC;

public class AMPClient {
    private final RequestSigner signer;
    private final SimpleHttpClient<String> client;
    private final String workspaceUrl;

    public AMPClient(Region region, String workspaceId) {
        this(region, workspaceId, DefaultCredentialsProvider.create());
    }

    @SuppressWarnings("resource")
    public AMPClient(Region region, String workspaceId, AwsCredentialsProvider credentialsProvider) {
        this(region, workspaceId, credentialsProvider, HttpClient.newHttpClient()::send);
    }

    AMPClient(
            Region region,
            String workspaceId,
            AwsCredentialsProvider credentialsProvider,
            SimpleHttpClient<String> client) {
        this.client = client;
        workspaceUrl = "https://aps-workspaces.%s.amazonaws.com/workspaces/%s".formatted(region, workspaceId);
        signer = new RequestSigner(credentialsProvider, Clock.system(UTC), Region.US_EAST_1);
    }

    public HttpResponse<String> writeSamples(Prometheus.WriteRequest req) throws IOException, InterruptedException {
        URI endpoint = getParameterizedUri("/api/v1/remote_write", Collections.emptyMap());
        byte[] compressed = Snappy.compress(req.toByteArray());
        var requestBuilder = createRequestBuilder(endpoint, signer.postRequest(endpoint, compressed));
        requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(compressed));
        return client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> httpRequest(String path, Map<String, String> parameters)
            throws IOException, InterruptedException {
        URI endpoint = getParameterizedUri(path, parameters);
        HttpRequest.Builder requestBuilder = createRequestBuilder(endpoint, signer.signGetRequest(endpoint));
        return client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpRequest.Builder createRequestBuilder(URI endpoint, SdkHttpFullRequest signedRequest) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(endpoint);
        for (var header : signedRequest.headers().entrySet()) {
            if (!header.getKey().equalsIgnoreCase("Host")) {
                requestBuilder.header(header.getKey(), header.getValue().getFirst());
            }
        }
        return requestBuilder;
    }

    private URI getParameterizedUri(String path, Map<String, String> parameters) {
        var location = workspaceUrl + path;
        if (parameters.isEmpty()) {
            return URI.create(location);
        }
        var queryParams = parameters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();
        return URI.create(location + "?" + String.join("&", queryParams));
    }
}
