## CI/CD Pipeline

This project uses Jenkins to automate the build, testing, containerization,
deployment, and verification process.

### Pipeline Stages

1. Checkout - Fetch source code from GitHub.
2. Build - Compile the Spring Boot application using Maven.
3. Test - Execute automated JUnit tests.
4. Docker Build - Create a multi-stage Docker image.
5. Docker Hub Push - Publish the image to Docker Hub.
6. Deploy - Deploy using Docker Compose.
7. Health Check - Verify application availability.

### Notifications

Jenkins sends email notifications through Gmail after successful
or failed pipeline executions.
