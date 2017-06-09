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

    if( config.get('uri') ) {
        url = config.uri + path
    }

    stage('Check Container Status') {
        def json = new groovy.json.JsonBuilder()
        def root = json secret: config['secret'], pid: config['pid']
        def jsonString = json.toString()
        // No longer needed remove since JsonBuilder is not serializable
        root = null
        json = null

        def results = new Utils().postUrl(url, jsonString, true)

        println("DEBUG: jsonString: ${jsonString}")
        println("DEBUG: Project ID: ${config['pid']}")
        println("DEBUG: Container Status Results: ${results}")
        println("DEBUG: rebuildJobName: ${config['rebuildJobName']}")

        if(results['rebuild'] == "none") {
            println("Rebuild is not necessary at this time.")
        }
        else {
            println("The container image needs to be rebuilt...")
            build job: config['rebuildJobName'],
                    parameters: config['rebuildJobParameters']
        }
    }
}
