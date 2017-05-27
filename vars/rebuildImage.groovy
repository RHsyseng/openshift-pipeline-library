#!groovy

import com.redhat.*

def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    String uri = "https://connect.redhat.com/api/container/status"
    def utils = new Utils()

    stage('Check Container Status') {
        def json = new groovy.json.JsonBuilder()
        def root = json secret: config['secret'], pid: config['pid']
        def results = utils.postUrl(uri, json.toString(), true)
        println(results)

        if(results['rebuild'] == "none") {
            println("don't rebuild")
        }
        else {
            println("rebuild")
            build job: config['rebuildJobName'],
                    parameters: config['rebuildJobParameters']
        }
    }
}