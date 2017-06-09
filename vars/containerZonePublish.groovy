#!groovy

import com.cloudbees.groovy.cps.NonCPS
import com.redhat.*

def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def dockerImageDigest = null
    def results = null

    String uri = "https://connect.redhat.com"
    String path = "/api/container/publish"
    String url = uri + path

    if( config.get('uri') ) {
        url = config.uri + path
    }

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${config.credentialsId}",
                      usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {

        /** Retrieve the docker image digest from the Red Hat Connect
         * OpenShift environment - which will be used with the scanning API.
         */
        stage('Retrieve Docker Digest') {
            openshift.withCluster( config.openShiftUri, env.PASSWORD ) {
                openshift.withProject( env.USERNAME ) {
                    def istagobj = openshift.selector( "istag/${config.imageName}:${config.imageTag}" ).object()
                    dockerImageDigest = istagobj.image.metadata.name
                }
            }
        }
        /** Create the payload and wait for results to be returned from
         * the API.  Once the certifications key is available break out of the loop
         * and continue.
         */
        stage('Wait for scan') {
            def json = new groovy.json.JsonBuilder()
            def root = json secret: env.PASSWORD, pid: env.USERNAME, docker_image_digest: dockerImageDigest
            def jsonString = json.toString()
            json = null
            root = null
            results = new Utils().postUrl(url, jsonString, true)

            if (results.containsKey("publish")) {
                if (results.publish.containsKey("success")){
                    if (results.publish.success) {
                        currentBuild.result = 'SUCCESS'
                    }
                }
            }
            else if (results.containsKey("errors")) {
                printErrorCriteria(results.errors.criteria)
                currentBuild.result = 'FAILURE'
            }
            else {
                println("Unknown response")
            }
        }
    }
}

/** sortPrintScanResults
 * Extracts the required and optional items for certification.
 * Determines if there are any items that failed in the required scanning
 * @param results
 * @return boolean
 */
@NonCPS
def printErrorCriteria(def results) {
    results.fail.each {
        println("FAILED - Please Review: ${it.label}\n${it.url}")
    }
}

