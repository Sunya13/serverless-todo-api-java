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
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lambda function to retrieve all to-do items from the DynamoDB table.
 * This function is triggered by a GET request to the API Gateway.
 */
public class GetAllTodosHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final DynamoDbTable<TodoItem> todoTable;

    public GetAllTodosHandler() {
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
            // Use the GSI to get all items
            QueryConditional queryConditional = QueryConditional.keyEqualTo(k -> k.partitionValue("TODO"));
            List<TodoItem> allTodos = todoTable.index("UpdatedAtIndex").query(queryConditional)
                    .stream()
                    .flatMap(page -> page.items().stream())
                    .collect(Collectors.toList());

            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(allTodos));
            response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
        } catch (Exception e) {
            context.getLogger().log("Error getting all todo items: " + e);
            response.setStatusCode(500);
            response.setBody("{\"error\": \"Internal server error. " + e + "\"}");
        }
        return response;
    }
}
