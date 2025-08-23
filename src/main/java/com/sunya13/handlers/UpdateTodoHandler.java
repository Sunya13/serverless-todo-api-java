package com.sunya13.handlers;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunya13.model.TodoItem;
import com.sunya13.utils.DynamoDbClientProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;

/**
 * Lambda function to handle the update of a to-do item.
 * This function is triggered by a PUT request to the API Gateway.
 */
public class UpdateTodoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final DynamoDbTable<TodoItem> todoTable;

    public UpdateTodoHandler() {
        this.objectMapper = new ObjectMapper();
        String tableName = System.getenv("TABLE_NAME");
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalStateException("FATAL: TABLE_NAME environment variable is not set.");
        }
        DynamoDbClient ddb = DynamoDbClientProvider.getClient();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(ddb).build();
        this.todoTable = enhancedClient.table(tableName, TableSchema.fromBean(TodoItem.class));
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            String todoId = request.getPathParameters().get("todoId");
            TodoItem updates = objectMapper.readValue(request.getBody(), TodoItem.class);

            TodoItem existingItem = todoTable.getItem(Key.builder().partitionValue(todoId).build());
            if (existingItem == null) {
                response.setStatusCode(404);
                response.setBody("{\"error\": \"Item not found.\"}");
                return response;
            }

            // Apply updates
            existingItem.setCompleted(updates.getCompleted());
            existingItem.setUpdatedAt(java.time.Instant.now().toString());

            todoTable.updateItem(existingItem);

            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(existingItem));
            response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
        } catch (Exception e) {
            context.getLogger().log("Error updating todo item: " + e);
            response.setStatusCode(500);
            response.setBody("{\"error\": \"Internal server error due to " + e + ".\"}");
        }
        return response;
    }
}
