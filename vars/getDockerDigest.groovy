#!groovy

def call(String secret, Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    stage('Retrieve Docker Digest') {
        openshift.withCluster( config.openShiftUri, secret ) {
		    openshift.withProject( config.openShiftProject ) {
        	    def istagobj = openshift.selector( "istag/${config.imageName}:${config.imageTag}" ).object()
        	    return istagobj.image.metadata.name
            }
   	    }
    }
}

def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'container-zone',
                      usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
        stage('Retrieve Docker Digest') {
            openshift.withCluster( config.openShiftUri, env.PASSWORD ) {
                openshift.withProject( env.USERNAME ) {
                    def istagobj = openshift.selector( "istag/${config.imageName}:${config.imageTag}" ).object()
                    return istagobj.image.metadata.name
                }
            }
        }
    }
}