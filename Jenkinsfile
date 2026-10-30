```groovy
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        DOCKERHUB_REPO = 'spe-calculator'
        DOCKERHUB_CREDENTIALS = 'dockerhub-credentials'
        EMAIL_RECIPIENT = 'jindal9547@gmail.com'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -e
                    chmod +x mvnw
                    ./mvnw -B clean package -DskipTests
                '''
            }
        }

        stage('Test') {
            steps {
                sh './mvnw -B test'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml',
                          allowEmptyResults: false
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    set -e
                    IMAGE="docker.io/manavimt2023535/spe-calculator:${BUILD_NUMBER}"
                    echo "Building image: $IMAGE"
                    docker build --pull -t "$IMAGE" .
                    docker image inspect "$IMAGE" --format='Image ID: {{.Id}}'
                '''
            }
        }

        stage('Docker Hub Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DH_USER',
                        passwordVariable: 'DH_TOKEN'
                    )
                ]) {
                    sh '''
                        set -e
                        set +x
                        IMAGE="docker.io/manavimt2023535/spe-calculator:${BUILD_NUMBER}"
                        echo "$DH_TOKEN" | docker login -u "$DH_USER" --password-stdin
                        docker push "$IMAGE"
                        docker logout
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -e
                    IMAGE="docker.io/manavimt2023535/spe-calculator:${BUILD_NUMBER}"
                    CALCULATOR_IMAGE="$IMAGE" docker compose -p calculator pull calculator
                    CALCULATOR_IMAGE="$IMAGE" docker compose -p calculator up -d --no-build --force-recreate
                '''
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    set -e
                    for attempt in $(seq 1 30); do
                        if curl --fail --silent http://localhost:8081/actuator/health; then
                            echo
                            echo "Calculator deployment is healthy."
                            exit 0
                        fi

                        echo "Waiting for application... ($attempt/30)"
                        sleep 5
                    done

                    echo "Health check failed."
                    docker compose -p calculator ps
                    docker compose -p calculator logs --tail=100
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            echo 'SPE Calculator pipeline completed successfully.'

            script {
                if (env.EMAIL_RECIPIENT?.trim()) {
                    emailext(
                        to: env.EMAIL_RECIPIENT,
                        subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        mimeType: 'text/html',
                        body: """
                            <h2>Build Successful!</h2>
                            <p><b>Project:</b> SPE Calculator CI/CD</p>
                            <p><b>Job:</b> ${env.JOB_NAME}</p>
                            <p><b>Build Number:</b> #${env.BUILD_NUMBER}</p>
                            <p><b>Status:</b> SUCCESS</p>
                            <p><b>Docker Image:</b> docker.io/manavimt2023535/spe-calculator:${env.BUILD_NUMBER}</p>
                            <p><b>Build URL:</b> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                        """
                    )
                    echo "Success notification sent to ${env.EMAIL_RECIPIENT}"
                } else {
                    echo 'EMAIL_RECIPIENT is not configured. Skipping notification.'
                }
            }
        }

        failure {
            echo 'SPE Calculator pipeline failed.'

            script {
                if (env.EMAIL_RECIPIENT?.trim()) {
                    emailext(
                        to: env.EMAIL_RECIPIENT,
                        subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        mimeType: 'text/html',
                        body: """
                            <h2>Build Failed!</h2>
                            <p><b>Project:</b> SPE Calculator CI/CD</p>
                            <p><b>Job:</b> ${env.JOB_NAME}</p>
                            <p><b>Build Number:</b> #${env.BUILD_NUMBER}</p>
                            <p><b>Status:</b> FAILURE</p>
                            <p><b>Build URL:</b> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                            <p>Please check the Jenkins console output for details.</p>
                        """
                    )
                    echo "Failure notification sent to ${env.EMAIL_RECIPIENT}"
                } else {
                    echo 'EMAIL_RECIPIENT is not configured. Skipping notification.'
                }
            }
        }

        always {
            echo "Finished build #${env.BUILD_NUMBER}"
        }
    }
}
```
