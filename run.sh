#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration ---
export AWS_ACCESS_KEY_ID="test"
export AWS_SECRET_ACCESS_KEY="test"
export AWS_DEFAULT_REGION="us-east-1"
export ENDPOINT_URL="http://localstack:4566"
export TABLE_NAME="TodoItems"
export GSI_NAME="UpdatedAtIndex"
export LAMBDA_ROLE="arn:aws:iam::000000000000:role/lambda-role"
export JAR_FILE="target/serverless-todo-api-java-1.00.00.jar"

# --- Main Script Execution ---

echo "Building Java application..."
mvn clean package

echo "Waiting for LocalStack to be ready..."
# Wait for the DynamoDB service to be available
until curl -s $ENDPOINT_URL/_localstack/health | grep -q '"dynamodb": "available"'; do
    sleep 5
done
echo "LocalStack is ready."

echo "Creating DynamoDB table..."
aws dynamodb create-table \
    --table-name $TABLE_NAME \
    --attribute-definitions AttributeName=todoId,AttributeType=S AttributeName=updatedAt,AttributeType=S AttributeName=GSI_PK,AttributeType=S \
    --key-schema AttributeName=todoId,KeyType=HASH \
    --global-secondary-indexes "IndexName=$GSI_NAME,KeySchema=[{AttributeName=GSI_PK,KeyType=HASH},{AttributeName=updatedAt,KeyType=RANGE}],Projection={ProjectionType=ALL},ProvisionedThroughput={ReadCapacityUnits=5,WriteCapacityUnits=5}" \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --endpoint-url $ENDPOINT_URL \
    --region $AWS_DEFAULT_REGION

echo "Creating Lambda functions in LocalStack..."
# Function to create a lambda function from a jar file
create_lambda_from_jar() {
  local func_name=$1
  local handler_class=$2
  echo "  -> Creating function: $func_name with handler: $handler_class"
  aws lambda create-function \
      --function-name "$func_name" \
      --runtime java11 \
      --zip-file fileb://$JAR_FILE \
      --handler "$handler_class" \
      --timeout 30 \
      --memory-size 1024 \
      --role $LAMBDA_ROLE \
      --environment "Variables={JAVA_TOOL_OPTIONS=-Xmx512m,TABLE_NAME=${TABLE_NAME},AWS_ENDPOINT_URL=${ENDPOINT_URL},AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION}}" \
      --endpoint-url $ENDPOINT_URL \
      --region $AWS_DEFAULT_REGION
  echo "  -> Function created successfully!"
}


# Function to wait for a specific Lambda function to become active
wait_for_lambda() {
    local func_name=$1
    echo "  -> Waiting for function '$func_name' to be active..."
    until aws lambda get-function --function-name "$func_name" --endpoint-url "$ENDPOINT_URL" --region "$AWS_DEFAULT_REGION" | grep -q '"State": "Active"'; do
        sleep 5
    done
    echo "  -> Function '$func_name' is active."
}

# Create all four functions

# Create and wait for each function
create_lambda_from_jar "createTodoFunction" "com.sunya13.handlers.CreateTodoHandler::handleRequest"
wait_for_lambda "createTodoFunction"

create_lambda_from_jar "getAllTodosFunction" "com.sunya13.handlers.GetAllTodosHandler::handleRequest"
wait_for_lambda "getAllTodosFunction"

create_lambda_from_jar "updateTodoFunction" "com.sunya13.handlers.UpdateTodoHandler::handleRequest"
wait_for_lambda "updateTodoFunction"

create_lambda_from_jar "deleteTodoFunction" "com.sunya13.handlers.DeleteTodoHandler::handleRequest"
wait_for_lambda "deleteTodoFunction"

echo "--- All functions deployed successfully! Pausing before tests... ---"
sleep 10

# --- Testing API Endpoints ---
echo "--- Testing API Endpoints ---"

# Create a directory inside target to store test results
mkdir -p target/test-results

# --- Create a To-Do Item (POST) ---
echo "Testing POST /todos..."
aws lambda invoke \
    --function-name createTodoFunction \
    --payload '{"body":"{\"title\":\"Test with script\"}"}' \
    --endpoint-url $ENDPOINT_URL \
    --cli-binary-format raw-in-base64-out \
    target/test-results/create_response.json

CREATE_RESPONSE=$(cat target/test-results/create_response.json)
CREATE_STATUS_CODE=$(echo "$CREATE_RESPONSE" | jq -r '.statusCode')
if [ "$CREATE_STATUS_CODE" -ne 201 ]; then
    echo "Error: CreateTodoHandler failed with status code $CREATE_STATUS_CODE"
    echo "Response: $CREATE_RESPONSE"
    exit 1
fi
echo "  -> POST Test Passed!"
CREATED_TODO_ID=$(echo "$CREATE_RESPONSE" | jq -r '.body | fromjson | .todoId')

# Add a check to ensure the ID was created
if [ -z "$CREATED_TODO_ID" ] || [ "$CREATED_TODO_ID" == "null" ]; then
    echo "Error: Failed to create a new To-Do item. ID is null."
    exit 1
fi

# --- Get All To-Do Items (GET) ---
echo "Testing GET /todos..."
aws lambda invoke \
    --function-name getAllTodosFunction \
    --payload '{}' \
    --endpoint-url $ENDPOINT_URL \
    --cli-binary-format raw-in-base64-out \
    target/test-results/get_all_response.json

GET_RESPONSE=$(cat target/test-results/get_all_response.json)
GET_STATUS_CODE=$(echo "$GET_RESPONSE" | jq -r '.statusCode')
if [ "$GET_STATUS_CODE" -ne 200 ]; then
    echo "Error: GetAllTodosHandler failed with status code $GET_STATUS_CODE"
    echo "Response: $GET_RESPONSE"
    exit 1
fi
echo "  -> GET Test Passed!"

# --- Update the To-Do Item (PUT) ---
echo "Testing PUT /todos/{todoId}..."
UPDATE_PAYLOAD='{"pathParameters":{"todoId":"'$CREATED_TODO_ID'"},"body":"{\"completed\":true}"}'
aws lambda invoke \
    --function-name updateTodoFunction \
    --payload "$UPDATE_PAYLOAD" \
    --endpoint-url $ENDPOINT_URL \
    --cli-binary-format raw-in-base64-out \
    target/test-results/update_response.json

UPDATE_RESPONSE=$(cat target/test-results/update_response.json)
UPDATE_STATUS_CODE=$(echo "$UPDATE_RESPONSE" | jq -r '.statusCode')
if [ "$UPDATE_STATUS_CODE" -ne 200 ]; then
    echo "Error: UpdateTodoHandler failed with status code $UPDATE_STATUS_CODE"
    echo "Response: $UPDATE_RESPONSE"
    exit 1
fi
echo "  -> PUT Test Passed!"

# --- Delete the To-Do Item (DELETE) ---
echo "Testing DELETE /todos/{todoId}..."
DELETE_PAYLOAD='{"pathParameters":{"todoId":"'$CREATED_TODO_ID'"}}'
aws lambda invoke \
    --function-name deleteTodoFunction \
    --payload "$DELETE_PAYLOAD" \
    --endpoint-url $ENDPOINT_URL \
    --cli-binary-format raw-in-base64-out \
    target/test-results/delete_response.json

DELETE_RESPONSE=$(cat target/test-results/delete_response.json)
DELETE_STATUS_CODE=$(echo "$DELETE_RESPONSE" | jq -r '.statusCode')
if [ "$DELETE_STATUS_CODE" -ne 200 ]; then
    echo "Error: DeleteTodoHandler failed with status code $DELETE_STATUS_CODE"
    echo "Response: $DELETE_RESPONSE"
    exit 1
fi
echo "  -> DELETE Test Passed!"

echo "--- All tests completed successfully! ---"
