package com.aquatic.amp.exporter;

import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.time.Clock;

class RequestSigner {
    private static final String SERVICE_NAME = "aps";
    private final AwsCredentialsProvider credentialsProvider;
    private final Clock clock;
    private final Region region;

    RequestSigner(AwsCredentialsProvider credentialsProvider, Clock clock, Region region) {
        this.credentialsProvider = credentialsProvider;
        this.clock = clock;
        this.region = region;
    }

    SdkHttpFullRequest signGetRequest(URI requestURI) {
        AwsCredentials awsCredentials = credentialsProvider.resolveCredentials();
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.GET)
                .uri(requestURI)
                .appendHeader("Host", requestURI.getHost())
                .build();
        Aws4Signer signer = Aws4Signer.create();
        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(awsCredentials)
                .signingName(SERVICE_NAME)
                .signingRegion(region)
                .signingClockOverride(clock)
                .build();
        return signer.sign(request, signerParams);
    }

    SdkHttpFullRequest postRequest(URI requestURI, byte[] requestBody) {
        AwsCredentials awsCredentials = credentialsProvider.resolveCredentials();

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.POST)
                .uri(requestURI)
                .appendHeader("Content-Type", "application/x-protobuf")
                .appendHeader("Host", requestURI.getHost())
                .appendHeader("Content-Encoding", "snappy")
                .contentStreamProvider(() -> SdkBytes.fromByteArray(requestBody).asInputStream())
                .build();

        Aws4Signer signer = Aws4Signer.create();
        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(awsCredentials)
                .signingName(SERVICE_NAME)
                .signingRegion(region)
                .signingClockOverride(clock)
                .build();

        return signer.sign(request, signerParams);
    }
}
