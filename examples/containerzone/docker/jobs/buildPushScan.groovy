#!groovy

@Library('Utils')
import com.redhat.*

node {
    stage('checkout') {
        checkout scm
    }

    /* NOTE: think about ISV testing...
     * How to inject a job to be built
     */
    dockerBuildPush {
        credentialsId = "ContainerZone"
        contextDir = "examples/docker"
        imageName = "czone"
        imageTag = "latest"
    }

    containerZoneScan {
        credentialsId = "ContainerZone"
        openShiftUri = "insecure://api.rhc4tp.openshift.com"
        imageName = "czone"
        imageTag = "latest"
    }
}
