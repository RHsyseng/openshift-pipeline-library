#!groovy
@Library('Utils')
import com.redhat.*

/** This pipeline job uses wiremock to test the vars associated with
 * the Container Zone.
 * see https://github.com/jcpowermac/openshift-wiremock
 */


node {

    def jobParameters = new JenkinsUtils().createJobParameters([name: "foo"])

    containerZoneScan {
        uri = "http://wiremock.router.default.svc.cluster.local"
        credentialsId = "ContainerZone"
        openShiftUri = "insecure://wiremock-ssl.router.default.svc.cluster.local"
        imageName = "starter-arbitrary-uid"
        imageTag = "1.0"
    }

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ContainerZone" , usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {

        def password = env.PASSWORD
        def username = env.USERNAME
        def jobName = "mockRebuild"

        rebuildImage {
            uri = "http://wiremock.router.default.svc.cluster.local"
            pid = username
            secret = password
            rebuildJobName = jobName
            rebuildJobParameters = jobParameters
        }
    }

    containerZonePublish {
        uri = "http://wiremock.router.default.svc.cluster.local"
        credentialsId = "ContainerZone"
        openShiftUri = "insecure://wiremock-ssl.router.default.svc.cluster.local"
        imageName = "starter-arbitrary-uid"
        imageTag = "1.0"
    }
}
