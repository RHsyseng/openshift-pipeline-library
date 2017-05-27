#!groovy

@Library('Utils')
import com.redhat.*


node {

    def registrySecret = "${secret}"

    def jobParameters = new JenkinsUtils().createJobParameters([name: "foo"])


    rebuildImage {
        pid = "p17633880910e488f5949aab3ad76cd4317542a7a06"
        secret = registrySecret
        rebuildJobName = "foo"
        rebuildJobParameters = jobParameters
    }
}