# serverless-todo-api-java/Dockerfile

# Use a suitable base image with a JDK pre-installed
FROM openjdk:11-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Install dependencies and AWS CLI v2
RUN apt-get update && apt-get install -y maven curl unzip && \
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm -rf awscliv2.zip aws

# Copy the entire project directory into the container
COPY . /app

# Set the entrypoint to a shell so the script can be executed
ENTRYPOINT ["/bin/bash"]

# This is the command that will be executed by docker-compose
CMD ["/app/run.sh"]