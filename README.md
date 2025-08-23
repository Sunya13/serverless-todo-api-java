# Serverless TODO API (Java)

This project is a complete, serverless RESTful API for managing a "TODO" list. It is built with Java 11 and designed for deployment on AWS Lambda, using Amazon DynamoDB as its data store.
A key feature is its robust local development and testing environment, powered by Docker and LocalStack. This allows developers to build, test, and debug the entire application on their local machine without needing an active AWS account.

### 🚀 Architecture Overview

The application consists of four separate Lambda functions that provide standard CRUD (Create, Read, Update, Delete) operations for TODO items.
* CreateTodoHandler: Handles POST requests to create a new item.
* GetAllTodosHandler: Handles GET requests to retrieve all items.
* UpdateTodoHandler: Handles PUT requests to modify an existing item.
* DeleteTodoHandler: Handles DELETE requests to remove an item.
* These functions interact with a DynamoDB table named TodoItems, which uses a Global Secondary Index (UpdatedAtIndex) for efficient querying.


### 📋 Prerequisites

Before you begin, ensure you have the following tools installed and configured:
* Java 11 or later
* Apache Maven
* Docker & Docker Compose
* AWS CLI

### 🛠️ How to Build

The project is built using Apache Maven. To compile the source code and package it into a distributable JAR file, run the following command from the root directory:

Bash

```mvn clean package```


This command will generate the fat JAR in the target/directory, which is named serverless-todo-api-java-1.00.00.jar.

### 🧪 Local Testing & Validation

This project is designed to be fully tested on your local machine using Docker Compose and LocalStack. The entire testing process is automated by the run.sh script.
To run the full suite of integration tests:
Start the test environment:

Bash

```docker compose up --build```


This single command will:
* Build the lambda-runner Docker image.
* Start a LocalStack container to emulate AWS Lambda and DynamoDB.
* Execute the run.sh script, which automatically:
* Builds the Java application using Maven.
* Creates the DynamoDB table and GSI.
* Deploys all four Lambda functions to LocalStack.
* Invokes each Lambda function to test the create, get, update, and delete operations.
* Stores the JSON output of each test in the target/test-results/ directory.
* You can view the real-time output of the tests by running: docker logs -f lambda-runner.

### 🐳 Docker Compose Overview

The docker-compose.yml file defines the services needed to run the local development and testing environment. Here's a breakdown of the services:

#### localstack

**Image**: localstack/localstack:latest

**Description**: This service emulates AWS services locally. For this project, it's configured to run Lambda and DynamoDB.

**Ports**: The default LocalStack port 4566 is exposed to the host machine.

**Volumes**:
./.localstack:/var/lib/localstack: Persists LocalStack data to avoid re-creating resources on every startup.
/var/run/docker.sock:/var/run/docker.sock: Allows LocalStack to interact with the Docker daemon, which is necessary for running Lambda functions in Docker containers.

#### lambda-runner

**Build:** This service is built from the Dockerfile in the project's root directory.
Description: This service is responsible for running the integration tests. It depends on the localstack service to be running.

**Environment:**
AWS_ENDPOINT_URL=http://localstack:4566: This tells the AWS CLI and SDK to send requests to the LocalStack container instead of the real AWS services.

**Volumes:**
.:/app: Mounts the current directory into the container, allowing the script to access the project files.
Command: The run.sh script is executed when the container starts.


### ⚙️ Environment Variables

The application and its test environment are configured using environment variables defined in run.sh.

Of course. Here is the environment variables table formatted as plain text for your `README.md` file.

```markdown
| Variable                  | Description                                       | Default Value (for local testing)              |
| `AWS_ACCESS_KEY_ID`       | Your AWS access key.                              | `"test"`                                       |
| `AWS_SECRET_ACCESS_KEY`   | Your AWS secret key.                              | `"test"`                                       |
| `AWS_DEFAULT_REGION`      | The AWS region to use.                            | `"us-east-1"`                                  |
| `ENDPOINT_URL`            | The endpoint URL for the AWS services.            | `"http://localstack:4566"`                     |
| `TABLE_NAME`              | The name of the DynamoDB table.                   | `"TodoItems"`                                  |
| `GSI_NAME`                | The name of the Global Secondary Index.           | `"UpdatedAtIndex"`                             |
| `LAMBDA_ROLE`             | The ARN of the IAM role for the Lambda functions. | `"arn:aws:iam::000000000000:role/lambda-role"` |
| `JAR_FILE`                | The path to the compiled JAR file.                | `"target/serverless-todo-api-java-1.00.00.jar"`|
```


### 🔄 CI/CD Pipeline

This project includes a Continuous Integration (CI) pipeline using GitHub Actions (as seen in .github/workflows/ci.yaml). On every push or pull request to the main branch, the workflow will automatically execute the same build and test steps described above to ensure code quality and reliability.
