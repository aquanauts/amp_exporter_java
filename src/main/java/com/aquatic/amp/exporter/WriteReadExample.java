package com.aquatic.amp.exporter;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.time.Instant;
import java.util.Map;

public class WriteReadExample {
    public static void main(String[] args) throws Exception {
        String workspaceId = args[0];
        String sampleName = "TestSample";
        if (args.length > 1) {
            sampleName = args[1];
        }
        var client = new AMPClient(Region.US_EAST_1, workspaceId, DefaultCredentialsProvider.create());
        var start = Instant.now().toString();
        for (int i = 0; i < 9; i++) {
            long now = System.currentTimeMillis();
            long value = now % 100;
            System.out.println("Writing sample: " + i + "=" + value + " at timestamp " + now);
            Prometheus.Sample sample = Prometheus.Sample.newBuilder().setTimestamp(now).setValue(value).build();
            Prometheus.Label label = Prometheus.Label.newBuilder().setName("__name__").setValue(sampleName).build();
            Prometheus.TimeSeries timeSeries = Prometheus.TimeSeries.newBuilder().addLabels(label).addSamples(sample).build();
            var response = client.writeSamples(Prometheus.WriteRequest.newBuilder().addTimeseries(timeSeries).build());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to write sample: " + i + "=" + response.body());
            }
            Thread.sleep(5000);
        }
        var end = Instant.now().toString();
        var results = client.httpRequest("/api/v1/query_range", Map.of("query", sampleName, "start", start, "end", end, "step", "15s"));
        if (results.statusCode() == 200) {
            System.out.println(results.body());
        } else {
            throw new RuntimeException("Failed to run query: " + results.body());
        }
    }
}
