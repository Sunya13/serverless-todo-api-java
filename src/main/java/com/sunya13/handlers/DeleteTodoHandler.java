package com.sunya13.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sunya13.model.TodoItem;
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

    private final DynamoDbClient ddb = DynamoDbClient.builder().build();
    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(ddb).build();
    private final String tableName = System.getenv("TABLE_NAME");
    private final DynamoDbTable<TodoItem> todoTable = enhancedClient.table(tableName, TableSchema.fromBean(TodoItem.class));

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        String todoId = request.getPathParameters().get("todoId");

        try {
            // Build the key for the item to delete
            Key key = Key.builder().partitionValue(todoId).build();

            // Delete the item from DynamoDB
            TodoItem deletedItem = todoTable.deleteItem(key);

            if (deletedItem == null) {
                response.setStatusCode(404); // Not Found
                response.setBody("{\"error\": \"To-Do item not found.\"}");
                return response;
            }

            response.setStatusCode(204); // No Content
        } catch (Exception e) {
            context.getLogger().log("Error deleting todo item: " + e.getMessage());
            response.setStatusCode(500); // Internal Server Error
            response.setBody("{\"error\": \"Internal server error.\"}");
        }
        return response;
    }
}

