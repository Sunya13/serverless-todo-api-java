package com.sunya13.handlers;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunya13.model.TodoItem;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;

/**
 * Lambda function to handle the creation of a new to-do item.
 * This function is triggered by a POST request to the API Gateway.
 */
public class CreateTodoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DynamoDbClient ddb = DynamoDbClient.builder().build();
    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(ddb).build();
    private final String tableName = System.getenv("TABLE_NAME");
    private final DynamoDbTable<TodoItem> todoTable = enhancedClient.table(tableName, TableSchema.fromBean(TodoItem.class));

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            // Deserialize the request body into a TodoItem object
            TodoItem newTodo = objectMapper.readValue(request.getBody(), TodoItem.class);

            // Set additional attributes and save to DynamoDB
            newTodo.setTodoId(java.util.UUID.randomUUID().toString());
            newTodo.setCompleted(false); // Default to false for new items
            newTodo.setCreatedAt(java.time.Instant.now().toString());
            newTodo.setUpdatedAt(java.time.Instant.now().toString());

            todoTable.putItem(newTodo);

            // Return a success response
            response.setStatusCode(201); // 201 Created
            response.setBody(objectMapper.writeValueAsString(newTodo));
            response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
        } catch (JsonProcessingException e) {
            context.getLogger().log("Error processing JSON: " + e.getMessage());
            response.setStatusCode(400); // Bad Request
            response.setBody("{\"error\": \"Invalid JSON in request body.\"}");
        } catch (Exception e) {
            context.getLogger().log("Error creating todo item: " + e.getMessage());
            response.setStatusCode(500); // Internal Server Error
            response.setBody("{\"error\": \"Internal server error.\"}");
        }
        return response;
    }
}