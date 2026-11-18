
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
