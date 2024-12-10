package com.aquatic.amp.exporter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

class RequestSignerTest {

    private RequestSigner signer;

    @BeforeEach
    public void beforeEach() {
        AwsCredentialsProvider credentialsProvider = () -> AwsBasicCredentials.create("akid", "skid");
        signer = new RequestSigner(credentialsProvider, Clock.fixed(Instant.ofEpochMilli(1234567890123L), UTC), Region.US_EAST_1);
    }

    @Test
    public void canSignGetRequests() {
        var request = signer.signGetRequest(URI.create("https://example.com"));
        Map<String, List<String>> headers = request.headers();
        assertEquals(List.of("example.com"), headers.get("Host"));
        assertEquals(List.of("20090213T233130Z"), headers.get("X-Amz-Date"));

        var authHeaderSections = headers.get("Authorization").getFirst().split(" ");
        assertEquals("AWS4-HMAC-SHA256", authHeaderSections[0]);
        assertEquals("Credential=akid/20090213/us-east-1/aps/aws4_request,", authHeaderSections[1]);
        assertEquals("SignedHeaders=host;x-amz-date,", authHeaderSections[2]);
        assertThat(authHeaderSections[3], containsString("Signature="));
    }

    @Test
    public void canSignPostRequests() {
        var request = signer.postRequest(URI.create("https://example.com"), "hello".getBytes());
        Map<String, List<String>> headers = request.headers();
        assertEquals(List.of("example.com"), headers.get("Host"));
        assertEquals(List.of("20090213T233130Z"), headers.get("X-Amz-Date"));
        assertEquals(List.of("application/x-protobuf"), headers.get("Content-Type"));
        assertEquals(List.of("snappy"), headers.get("Content-Encoding"));

        var authHeaderSections = headers.get("Authorization").getFirst().split(" ");
        assertEquals("AWS4-HMAC-SHA256", authHeaderSections[0]);
        assertEquals("Credential=akid/20090213/us-east-1/aps/aws4_request,", authHeaderSections[1]);
        assertEquals("SignedHeaders=content-encoding;content-type;host;x-amz-date,", authHeaderSections[2]);
        assertThat(authHeaderSections[3], containsString("Signature="));
    }
}