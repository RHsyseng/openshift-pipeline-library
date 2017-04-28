#!groovy

import com.redhat.Utils

def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def pod = null
    def podObject = null
    boolean deletePod = config['deletePod'] ?: false
    def args = []

    args << "${config.branch}"
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
                    /* Arguments to openshift.run must be expanded using the spread operator
                       which does not work with CPS.  Created static function in Utils to
                       get around this limitation.

                       This will not work:
                       openshift.run(args)
                     */
                    new Utils().openShiftRun(openshift, args)
                    pod = openshift.selector("pod/${config.branch}")

                    timeout(10) {
                        pod.watch {
                            podObject = it.object()
                            if (podObject.status.phase == 'Succeeded' || podObject.status.phase == 'Failed') {
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
                        def exitCode = podObject.status.containerStatuses[0].state.terminated.exitCode
                        def result = pod.logs()
                        echo "status: ${result.status}"
                        echo "${result.actions[0].cmd}"

                        if (exitCode != 0) {
                            echo "${result.out}"
                            currentBuild.result = 'FAILURE'
                        }
                        if (deletePod) {
                            pod.delete()
                        }
                    }
                }
            }
        }
    }
}


