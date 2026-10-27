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
        CALCULATOR_IMAGE = ''
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
                script {
                    env.CALCULATOR_IMAGE =
                        "docker.io/manavimt2023535/${env.DOCKERHUB_REPO}:${env.BUILD_NUMBER}"
                }

                sh '''
                    docker build --pull -t "$CALCULATOR_IMAGE" .
                    docker image inspect "$CALCULATOR_IMAGE" --format='Image ID: {{.Id}}'
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
                        set +x
                        echo "$DH_TOKEN" | docker login -u "$DH_USER" --password-stdin
                        IMAGE="docker.io/manavimt2023535/spe-calculator:${BUILD_NUMBER}"
                        docker push "$IMAGE"
                        docker logout
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    IMAGE="docker.io/manavimt2023535/spe-calculator:${BUILD_NUMBER}"
                    CALCULATOR_IMAGE="$IMAGE" docker compose -p calculator pull calculator
                    CALCULATOR_IMAGE="$IMAGE" docker compose -p calculator up -d --no-build --force-recreate
                '''
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    for attempt in $(seq 1 30); do
                        if curl --fail --silent \
                            http://localhost:8081/actuator/health; then
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
                        body: """Build succeeded.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Image: ${env.CALCULATOR_IMAGE}
Details: ${env.BUILD_URL}
"""
                    )
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
                        body: """Build failed.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Details: ${env.BUILD_URL}
Check the Jenkins console output for details.
"""
                    )
                }
            }
        }

        always {
            echo "Finished build #${env.BUILD_NUMBER}"
        }
    }
}
