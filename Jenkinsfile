pipeline {
    agent any

    stages {
        stage('Build and scan') {
            steps {
                script{
                    withSonarQubeEnv("sonarqube"){
                        dir("${env.WORKSPACE}"){
                            bat '''
                                dir
                                dotnet sonarscanner begin /k:"project-key-jenkins" /d:sonar.login="c9be09a62c4fed84d746a12827b844efad92ed85"
                                dotnet build
                                dotnet sonarscanner end
                            '''
                        }
                    }
                }
            }
            
        }
    }
}
