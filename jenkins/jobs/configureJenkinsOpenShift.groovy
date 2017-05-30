#!groovy

@Library('Utils')
import com.redhat.*

node {
    def id = null
    def seedJobParameters = null
    def jenkinsUtils = new com.redhat.JenkinsUtils()

    /* The Jenkins root url is configured under
     * Manage Jenkins -> Configure System -> Jenkins Location
     * This is not configured properly in a ephemeral deployment.
     * Use the OpenShift route object to determine the correct url.
     */
    stage('Configure URL') {
        openshift.withCluster() {
            def route = openshift.selector('route', 'jenkins').object()
            jenkinsUtils.configureRootUrl("https://${route.spec.host}")
        }
    }
    stage('Extract ConfigMap') {
        openshift.withCluster() {
            def configMap = openshift.selector( "configmap/orgfolder" ).object().data
            seedJobParameters = jenkinsUtils.createJobParameters(configMap)
        }
    }

    stage('OpenShift -> Jenkins credentials') {
        openshift.withCluster() {
            def secret = openshift.selector( "secret/github" ).object()
            id = jenkinsUtils.createCredentialsFromOpenShift(secret, "github")
        }
    }

    stage('Configure Anonymous User') {
        jenkinsUtils.setAnonPermBuildStatusIcon()
    }

    stage('Run Seed Job') {            
        build job: 'gitHubOrgSeed', parameters: seedJobParameters
    }
}
