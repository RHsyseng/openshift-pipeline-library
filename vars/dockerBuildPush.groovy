#!groovy

def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    
    def image = null

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${config.credentialsId}" , usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
        docker.withRegistry('https://registry.rhc4tp.openshift.com', "${config.credentialsId}" ) {
            stage('Build Docker Image') {
                dir("${config.contextDir}") {
                    image = docker.build("${env.USERNAME}/${config.imageName}:${config.imageTag}")
                }
            }

            if( config.get('testJobName') ) {
                stage('Run ${config.testJobName}'){
                    build job: config.testJobName,
                            parameters: config.testJobParameters
                }
            }

            stage('Push Image') {
                image.push()
            }
        }
    }
}
