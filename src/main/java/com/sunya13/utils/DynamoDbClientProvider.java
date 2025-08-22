package com.sunya13.utils;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

public class DynamoDbClientProvider {
    public static DynamoDbClient getClient() {
        // Check for the endpoint override environment variable
        String endpointUrl = System.getenv("AWS_ENDPOINT_URL");
        if (endpointUrl != null && !endpointUrl.isEmpty()) {
            // If it exists, we're in a local environment (LocalStack)
            return DynamoDbClient.builder()
                    .endpointOverride(URI.create(endpointUrl))
                    // Region is still required by the SDK, even for local
                    .region(Region.of(System.getenv("AWS_DEFAULT_REGION")))
                    .build();
        } else {
            // Otherwise, build a default client for a real AWS environment
            return DynamoDbClient.builder().build();
        }
    }
}