#!groovy

import com.redhat.*

def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    String uri = "https://connect.redhat.com"
    String path = "/api/container/status"
    String url = uri + path

    if (config.get('uri')) {
        url = config.uri + path
    }

    withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: "${config.credentialsId}",
                      usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
        stage('Check Container Status') {
            def json = new groovy.json.JsonBuilder()
            def root = json secret: env.PASSWORD, pid: env.USERNAME
            def jsonString = json.toString()
            // No longer needed remove since JsonBuilder is not serializable
            root = null
            json = null

            def results = new Utils().postUrl(url, jsonString, true)

            println("DEBUG: Container Status Results: ${results}")

            if (results.containsKey("errors")) {
                println(results.errors)
                currentBuild.result = 'FAILURE'
            } else if (results.containsKey("rebuild")) {
                if (results.rebuild == "none") {
                    println("Rebuild is not necessary at this time.")
                } else if (results.rebuild == "recommended") {
                    println("The rebuild status is ${results.rebuild} and freshness grade: ${results.grade}")

                } else if (results.rebuild == "mandatory") {
                    println("The rebuild status is ${results.rebuild} and freshness grade: ${results.grade}")
                    if (config.get('rebuildJobName')) {
                        build job: config['rebuildJobName'],
                                parameters: config['rebuildJobParameters']
                    }
                } else {
                    error "ERROR Unknown response from container status: ${results}"
                    currentBuild.result = 'FAILURE'
                }
            }
        }
    }
}
