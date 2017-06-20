#!groovy
@Library('Utils')
import com.redhat.*

/** This pipeline job uses wiremock to test the vars associated with
 * the Container Zone.
 * see https://github.com/jcpowermac/openshift-wiremock
 */


node {
    def jobParameters = new JenkinsUtils().createJobParameters([name: "foo"])
    def jobName = "mockRebuild"

    containerZoneHealthCheck {
        uri = "http://wiremock.router.default.svc.cluster.local"
        credentialsId = "ContainerZone"
        rebuildJobName = jobName
        rebuildJobParameters = jobParameters
    }
}
