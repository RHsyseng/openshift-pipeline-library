#!groovy

@Library('Utils')
import com.redhat.*


node {

    /**
     * This DSL (groovy closure) checks the container status
     * rebuilding the image if required
     */
    def jobParameters = new JenkinsUtils().createJobParameters([name: "foo"])

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ContainerZone" , usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {

        def password = env.PASSWORD
        def username = env.USERNAME
        def jobName = JOBNAME

        rebuildImage {
            pid = username
            secret = password
            rebuildJobName = jobName
            rebuildJobParameters = jobParameters
        }
    }
}
