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
    /* Create Jenkins build job parameters from a OpenShift ConfigMap */
    stage('Extract ConfigMap') {
        openshift.withCluster() {
            def configMap = openshift.selector( "configmap/orgfolder" ).object().data
            seedJobParameters = jenkinsUtils.createJobParameters(configMap)
        }
    }

    /* Create a Jenkins credential from a OpenShift secret for GitHub user and token */
    stage('OpenShift -> Jenkins credentials') {
        openshift.withCluster() {
            def secret = openshift.selector( "secret/github" ).object()
            id = jenkinsUtils.createCredentialsFromOpenShift(secret, "github")
        }
    }
    /* To use the embeddable build status plugin the anonymous user must
       have the ability to ViewStatus and Read.
     */
    stage('Configure Anonymous User') {
        jenkinsUtils.setAnonPermBuildStatusIcon()
    }
    /* This builds the Seed job for the GitHub Organizational folder Job DSL
     * to create a job for the github org configured in seedJobParameters.
     */
    stage('Run Seed Job') {            
        build job: 'gitHubOrgSeed', parameters: seedJobParameters
    }
}
