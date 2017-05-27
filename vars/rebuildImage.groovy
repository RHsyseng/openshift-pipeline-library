#!groovy

import com.redhat.*

def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    String uri = "https://connect.redhat.com/api/container/status"

    stage('Check Container Status') {
        def json = new groovy.json.JsonBuilder()
        def root = json secret: config['secret'], pid: config['pid']
        def results = new Utils().postUrl(uri, json.toString(), true)
        json = null
        root = null

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