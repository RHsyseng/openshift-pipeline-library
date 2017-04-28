#!groovy

def call(String imageStreamName) {
    stage('OpenShift Get ImageStream') {
        openshift.withCluster() {
            openshift.withProject() {
                try {
                    def is = openshift.selector("is/${imageStreamName}").object()
                    String imageStream = "${is.metadata.namespace}/${is.metadata.name}"
                    HashMap isMap = [dockerImageRepository: is.status.dockerImageRepository,
                                     imageStream: imageStream]
                    return isMap
                }
                catch(all) {
                    currentBuild.result = 'FAILURE'
                }
            }
        }
    }
}
