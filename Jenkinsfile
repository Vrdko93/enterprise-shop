
pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Unit Test') {
            steps {
                bat 'mvn -B clean verify'
            }
        }

        stage('Verify Docker') {
            steps {
                bat 'docker --version'
                bat 'docker info --format "{{.ServerVersion}}"'
            }
        }


        stage('Build Docker Images') {
            steps {
                script {
                    def gitSha = bat(
                        script: '@git rev-parse --short=7 HEAD',
                        returnStdout: true
                    ).trim()

                    def services = [
                        'user-service',
                        'product-service',
                        'order-service',
                        'payment-service',
                        'notification-service',
                        'api-gateway'
                    ]

                    for (service in services) {
                        echo "Building ${service}:${gitSha}"

                        bat "docker build -f services/${service}/Dockerfile -t enterprise-shop/${service}:${gitSha} ."
                    }
                }
            }
        }

        stage('Push Docker Images') {
            steps {
                script {
                    def gitSha = bat(
                        script: '@git rev-parse --short=7 HEAD',
                        returnStdout: true
                    ).trim()

                    def services = [
                        'user-service',
                        'product-service',
                        'order-service',
                        'payment-service',
                        'notification-service',
                        'api-gateway'
                    ]

                    withCredentials([
                        usernamePassword(
                            credentialsId: 'dockerhub-credentials',
                            usernameVariable: 'DOCKERHUB_USER',
                            passwordVariable: 'DOCKERHUB_TOKEN'
                        )
                    ]) {
                        bat '@echo off && <nul set /p "=%DOCKERHUB_TOKEN%" | docker login --username "%DOCKERHUB_USER%" --password-stdin'R%" --password-stdin'

                        for (service in services) {
                            def localImage = "enterprise-shop/${service}:${gitSha}"
                            def remoteImage = "vrdko93/enterprise-shop-${service}:${gitSha}"

                            bat "docker tag ${localImage} ${remoteImage}"
                            bat "docker push ${remoteImage}"
                        }
                    }
                }
            }
        }

    }

    post {
        success {
            echo 'Enterprise Shop build and tests passed!'
        }
        failure {
            echo 'Enterprise Shop pipeline failed. Check the console output.'
        }
    }
}
