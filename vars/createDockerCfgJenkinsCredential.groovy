#!groovy

import com.redhat.*

/** createDockerCfgJenkinsCredential
 * Required:
 * secretName
 *
 * @param body
 * @return
 */
def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    stage('Retrieve DockerCfg OpenShift and Create in Jenkins') {
        openshift.withCluster() {
            def secret = openshift.selector( "secret/${config.secretName}" ).object()
            new JenkinsUtils().createCredentialsFromOpenShiftDockerCfg(secret, "${config.secretName}")
        }
    }
}
