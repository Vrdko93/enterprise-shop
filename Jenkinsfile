
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
