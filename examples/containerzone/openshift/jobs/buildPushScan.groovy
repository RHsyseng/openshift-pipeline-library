#!groovy

@Library('Utils')
import com.redhat.*

/*
 * TODO: Add build parameters
 * TODO: Maybe use newBuildOpenShift?
 */

node {

    /** vars/createDockerCfgJenkinsCredential.groovy
     * This DSl will create a Jenkins credential from
     * an OpenShift DockerCfg secret.
     */
    createDockerCfgJenkinsCredential {
        secretName = "ContainerZone"
    }

    stage('Start OpenShift Build') {
        openshiftBuild(buildConfig: "${EXTERNAL_REGISTRY_IMAGE_NAME}-ex-reg", showBuildLogs: 'true')
    }

    /** vars/containerZoneScan.groovy
     * This DSL will use the connect API to determine the status of the scan
     * and display the scan results with the Jenkins console.
     */
    containerZoneScan {
        credentialsId = "ContainerZone"
        openShiftUri = "insecure://api.rhc4tp.openshift.com"
        imageName = "czone"
        imageTag = "latest"
    }
}
