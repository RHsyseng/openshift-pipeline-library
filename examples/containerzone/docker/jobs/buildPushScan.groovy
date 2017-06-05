#!groovy

@Library('Utils')
import com.redhat.*

node {
    stage('checkout') {
        checkout scm
    }

    /** vars/dockerBuildPush.groovy
     * This DSL (groovy closure) will build the dockerfile located in the
     * contextDir.  Once created an optional parameters testJobName and testJobParameters
     * can be used to test the resulting image.  Finally the image will be pushed
     * to the partner registry.
     */
    dockerBuildPush {
        credentialsId = "ContainerZone"
        contextDir = "examples/docker"
        imageName = "czone"
        imageTag = "latest"
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
