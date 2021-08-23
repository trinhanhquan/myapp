pipeline {
    agent any

    stages {
        stage('Build and scan') {
            steps {
                script{
                    withSonarQubeEnv("sonarqube"){
                        dir("${env.WORKSPACE}"){
                            bat '''
                                
                                dotnet sonarscanner begin /k:"project-key-jenkins"
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
