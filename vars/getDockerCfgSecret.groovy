#!groovy

def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    stage('Retrieve DockerCfg Secret in OpenShift') {
        openshift.withCluster() {
            def secret = openshift.selector( "secret/${config.secretName}" ).object()
            return secret.data.'.dockercfg'
        }
    }
}
