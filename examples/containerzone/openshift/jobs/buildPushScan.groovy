#!groovy

@Library('Utils')
import com.redhat.*

/*
 * TODO: Add build parameters
 * TODO: Maybe use newBuildOpenShift?
 */

node {

    createDockerCfgJenkinsCredential {
        secretName = "ContainerZone"
    }

    stage('Start OpenShift Build') {
        openshiftBuild(buildConfig: "${EXTERNAL_REGISTRY_IMAGE_NAME}-ex-reg", showBuildLogs: 'true')
    }

    containerZoneScan {
        credentialsId = "ContainerZone"
        openShiftUri = "insecure://api.rhc4tp.openshift.com"
        imageName = "czone"
        imageTag = "latest"
    }
}
