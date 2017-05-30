#!groovy

@Library('Utils')
import com.redhat.*


node {

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