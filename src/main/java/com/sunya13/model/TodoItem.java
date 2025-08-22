package com.sunya13.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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

    public TodoItem() {
        this.todoId = UUID.randomUUID().toString();
        this.completed = false;
        this.createdAt = Instant.now().toString();
        this.updatedAt = this.createdAt;
    }

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
    public String getGSI_PK() {
        // A static value for the GSI partition key
        return "todo";
    }
}