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

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${config.credentialsId}",
                      usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
        stage('Retrieve Docker Digest') {
            openshift.withCluster( config.openShiftUri, env.PASSWORD ) {
                openshift.withProject( env.USERNAME ) {
                    def istagobj = openshift.selector( "istag/${config.imageName}:${config.imageTag}" ).object()
                    dockerImageDigest = istagobj.image.metadata.name
                }
            }
        }
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

            printScanResults(results["certifications"][0]["assessment"])
        }
    }
}

@NonCPS
def printScanResults(def results) {
    def requiredForCert = results.findAll{ it.value["required_for_certification"] }
    def optional = results.findAll{ !it.value["required_for_certification"] }
    requiredForCert.each {
        String name = it.name.replaceAll('_', ' ').minus(" exists").capitalize()
        if(it.value["value"]) {
            println("${name}: PASSED")
        }
        else {
            println("${name}: FAILED")
        }
    }
    optional.each {
        String name = it.name.replaceAll('_', ' ').minus(" exists").capitalize()
        if(it.value["value"]) {
            println("${name}: PASSED")
        }
        else {
            println("${name}: FAILED")
        }
    }
}