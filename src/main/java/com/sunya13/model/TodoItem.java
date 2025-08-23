package com.sunya13.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

/**
 * Represents a single to-do item in the DynamoDB table.
 * Uses annotations to map the Java object to a DynamoDB item.
 */
@DynamoDbBean
public class TodoItem {
    private String todoId;
    private String title;
    private Boolean completed;
    private String createdAt;
    private String updatedAt;
    private String GSI_PK; // Field for the GSI Partition Key

    @DynamoDbPartitionKey
    public String getTodoId() {
        return todoId;
    }

    public void setTodoId(String todoId) {
        this.todoId = todoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbSecondarySortKey(indexNames = "UpdatedAtIndex")
    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "UpdatedAtIndex")
    @DynamoDbAttribute("GSI_PK") // This annotation fixes the name mismatch
    public String getGSI_PK() {
        return GSI_PK;
    }

    public void setGSI_PK(String GSI_PK) {
        this.GSI_PK = GSI_PK;
    }
}
