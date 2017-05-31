#!groovy

@Library('Utils')
import com.redhat.*

node {
    stage('checkout') {
        checkout scm
    }

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
