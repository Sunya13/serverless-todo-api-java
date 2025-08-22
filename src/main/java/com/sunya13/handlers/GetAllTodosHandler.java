package com.sunya13.handlers;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunya13.model.TodoItem;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lambda function to retrieve all to-do items from the DynamoDB table.
 * This function is triggered by a GET request to the API Gateway.
 */
public class GetAllTodosHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DynamoDbClient ddb = DynamoDbClient.builder().build();
    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(ddb).build();
    private final String tableName = System.getenv("TABLE_NAME");
    private final String gsiName = System.getenv("GSI_NAME");
    private final DynamoDbTable<TodoItem> todoTable = enhancedClient.table(tableName, TableSchema.fromBean(TodoItem.class));

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            // Use the Global Secondary Index to query and sort the items by updatedAt in descending order
            DynamoDbIndex<TodoItem> updatedAtIndex = todoTable.index(gsiName);

            QueryConditional queryConditional = QueryConditional.keyEqualTo(k -> k.partitionValue("todo"));

            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                    .queryConditional(queryConditional)
                    .scanIndexForward(false) // false for descending order
                    .build();

            List<TodoItem> todos = updatedAtIndex.query(queryRequest).stream()
                    .flatMap(page -> page.items().stream())
                    .collect(Collectors.toList());

            // Return a success response with the list of items
            response.setStatusCode(200); // OK
            response.setBody(objectMapper.writeValueAsString(todos));
            response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
        } catch (Exception e) {
            context.getLogger().log("Error getting all todos: " + e.getMessage());
            response.setStatusCode(500); // Internal Server Error
            response.setBody("{\"error\": \"Internal server error.\"}");
        }
        return response;
    }
}
