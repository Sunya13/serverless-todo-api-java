package com.sunya13.handlers;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunya13.model.TodoItem;
import com.sunya13.utils.DynamoDbClientProvider; // Import the new provider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;

public class CreateTodoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final DynamoDbTable<TodoItem> todoTable;

    public CreateTodoHandler() {
        this.objectMapper = new ObjectMapper();
        String tableName = System.getenv("TABLE_NAME");
        if (tableName == null || tableName.isEmpty()) {
            // This will provide a clear error if the environment variable is missing
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
            TodoItem newTodo = objectMapper.readValue(request.getBody(), TodoItem.class);

            newTodo.setTodoId(java.util.UUID.randomUUID().toString());
            newTodo.setCompleted(false);
            newTodo.setCreatedAt(java.time.Instant.now().toString());
            newTodo.setUpdatedAt(java.time.Instant.now().toString());
            // This is needed for the GSI
            newTodo.setGSI_PK("TODO");

            todoTable.putItem(newTodo);

            response.setStatusCode(201);
            response.setBody(objectMapper.writeValueAsString(newTodo));
            response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
        } catch (JsonProcessingException e) {
            context.getLogger().log("Error processing JSON: " + e.getMessage());
            response.setStatusCode(400);
            response.setBody("{\"error\": \"Invalid JSON in request body.\"}");
        } catch (Exception e) {
            context.getLogger().log("Error creating todo item: " + e.toString());
            response.setStatusCode(500);
            response.setBody("{\"error\": \"Internal server error" + e + ".\"}");
        }
        return response;
    }
}
