package com.sunya13.handlers;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunya13.model.TodoItem;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;
import java.time.Instant;

/**
 * Lambda function to handle the update of a to-do item.
 * This function is triggered by a PUT request to the API Gateway.
 */
public class UpdateTodoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DynamoDbClient ddb = DynamoDbClient.builder().build();
    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(ddb).build();
    private final String tableName = System.getenv("TABLE_NAME");
    private final DynamoDbTable<TodoItem> todoTable = enhancedClient.table(tableName, TableSchema.fromBean(TodoItem.class));

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        String todoId = request.getPathParameters().get("todoId");

        try {
            // Find the item to update
            Key key = Key.builder().partitionValue(todoId).build();
            TodoItem todoToUpdate = todoTable.getItem(key);

            if (todoToUpdate == null) {
                response.setStatusCode(404); // Not Found
                response.setBody("{\"error\": \"To-Do item not found.\"}");
                return response;
            }

            // Parse the request body for updates
            JsonNode requestBody = objectMapper.readTree(request.getBody());
            if (requestBody.has("title")) {
                todoToUpdate.setTitle(requestBody.get("title").asText());
            }
            if (requestBody.has("completed")) {
                todoToUpdate.setCompleted(requestBody.get("completed").asBoolean());
            }

            // Update the updatedAt timestamp
            todoToUpdate.setUpdatedAt(Instant.now().toString());

            // Save the updated item back to DynamoDB
            todoTable.putItem(todoToUpdate);

            response.setStatusCode(200); // OK
            response.setBody(objectMapper.writeValueAsString(todoToUpdate));
            response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
        } catch (Exception e) {
            context.getLogger().log("Error updating todo item: " + e.getMessage());
            response.setStatusCode(500); // Internal Server Error
            response.setBody("{\"error\": \"Internal server error.\"}");
        }
        return response;
    }
}
