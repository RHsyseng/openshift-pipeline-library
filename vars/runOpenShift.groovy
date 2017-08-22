#!groovy

def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def pod = null
    def podObject = null
    boolean deletePod = config['deletePod'] ?: false
    def args = []


    String podName = config['name'] ?: ""
    /* The name of the pod must be alphanumeric lowercase with a hyphen or period */
    //def podName = config['branch'].replaceAll('_','-').toLowerCase()

    args << "${podName}"
    args << "--image=${config.image}"
    args << "--restart=Never"
    args << "--image-pull-policy=Always"

    if(config['env']) {
        /* BUG: config.env.each works but only
         * iterated through the first item in the list
         * DO NOT USE [].each
         */
        for(env in config.env) {
            args << "--env=\"${env}\""
            println(env)
        }
    }

    stage('OpenShift Run') {
        openshift.withCluster() {
            openshift.withProject() {
                try {

                    openshift.run(args)
                    pod = openshift.selector("pod/${podName}")

                    timeout(10) {
                        pod.watch {
                            podObject = it.object()
                            if (podObject.status.phase == 'Running' || podObject.status.phase == 'Succeeded' || podObject.status.phase == 'Failed') {
                                return true
                            } else {
                                return false
                            }
                        }
                    }
                }
                finally {
                    if (pod) {
                        podObject = pod.object()
                        def result = pod.logs()
                        //def exitCode = podObject.status.containerStatuses[0].state.terminated.exitCode
                        echo "status: ${result.status}"
                        echo "output: ${result.out}"

                        if (deletePod) {
                            pod.delete()
                        }
                    }
                }
            }
        }
    }
}
