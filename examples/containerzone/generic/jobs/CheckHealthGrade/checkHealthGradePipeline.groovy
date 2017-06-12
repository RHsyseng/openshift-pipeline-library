#!groovy

@Library('Utils')
import com.redhat.*


node {

    /**
     * This DSL (groovy closure) checks the container status
     * rebuilding the image if required
     */
    def jobParameters = new JenkinsUtils().createJobParameters([name: "foo"])

    def jobName = JOBNAME

    containerZoneHealthCheck {
        credentialsId = "ContainerZone"
        rebuildJobName = jobName
        rebuildJobParameters = jobParameters
    }
}
