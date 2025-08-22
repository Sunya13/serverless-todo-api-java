package com.sunya13.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sunya13.model.TodoItem;
import com.sunya13.utils.DynamoDbClientProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Lambda function to handle the deletion of a to-do item.
 * This function is triggered by a DELETE request to the API Gateway.
 */
public class DeleteTodoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbTable<TodoItem> todoTable;

    public DeleteTodoHandler() {
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
            todoTable.deleteItem(Key.builder().partitionValue(todoId).build());

            response.setStatusCode(200);
            response.setBody("{\"message\": \"Item deleted successfully.\"}");
        } catch (Exception e) {
            context.getLogger().log("Error deleting todo item: " + e.toString());
            response.setStatusCode(500);
            response.setBody("{\"error\": \"Internal server error.\"}");
        }
        return response;
    }
}
