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

    def uri = "https://connect.redhat.com/api/container/scanResults"
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

            timeout(30) {
                waitUntil {
                    results = new Utils().postUrl(uri, jsonString, true)

                    if (results.containsKey("certifications")) {
                        return true
                    }
                    else return false
                }
            }

            currentBuild.result = 'SUCCESS'
            if( sortPrintScanResults(results["certifications"][0]["assessment"]) ) {
                currentBuild.result = 'FAILURE'
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
def sortPrintScanResults(def results) {
    def requiredForCert = results.findAll{ it["required_for_certification"] }

    /** In the requiredForCert list findall items in the dictionary with the
     * key 'value' that is false.  Calculate the size as a boolean value.
     */
    def failed = requiredForCert.findAll( { !it.value } ).size().asBoolean()
    def optional = results.findAll{ !it["required_for_certification"] }

    printScanResults(requiredForCert)
    printScanResults(optional)

    return failed
}

@NonCPS
def printScanResults(def results) {
    results.each {
        String name = it.name.replaceAll('_', ' ').minus(" exists").capitalize()
        println("${name}: ${it.value ? "PASSED" : "FAILED"}")
    }
}