#!groovy
import java.util.UUID



def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def newBuild = null
    String contextDir = config['contextDir'] ?: ""
    String image = ""
    String name = config['branch'] ?: ""
    String imageStream = ""

    if(config['image']) {
        image = "${config.image}~"
    }

    if(config['imageStream']) {
        imageStream = "--image-stream=${config.imageStream}"
    }

    if(config['randomName']) {
        name = UUID.randomUUID().toString()
        if(name[0].isNumber()) {
            name = name.replaceAll(name[0], 'a')
        }
    }

    def deleteBuild = config['deleteBuild'] ?: false

    stage('OpenShift Build') {
        openshift.withCluster() {
            openshift.withProject() {
                try {
                    def builds = null
                    def buildConfigName = null

                    /* If the OpenShift oc new-build command is ran in succession it can cause a
                     * race if the base imagestream does not already exist.  In normal situations
                     * this is not a problem but in Jenkins when multiple jobs could be occurring
                     * simultaneously this will happen.  Adding a lock in this section resolves
                     * that issue.
                     */

                    lock(resource: 'openshift.newBuild', inversePrecedence: true) {
                        /* Use oc new-build to build the image using the clone_url and ref
                         * TODO: Determine a method to new-build with a "Dockerfile" with a
                         * TODO: different filename e.g. Dockerfile.rhel7.
                         */

                        newBuild = openshift.newBuild("${image}${config.url}#${config.branch}",
                                "--name=${name}",
                                "--context-dir=${contextDir}",
                                "${imageStream}")
                        echo "newBuild created: ${newBuild.count()} objects : ${newBuild.names()}"

                        def buildConfig = newBuild.narrow("bc")
                        buildConfigName = buildConfig.object().metadata.name

                        builds = buildConfig.related("builds")
                        timeout(5) {
                            builds.watch {
                                if (it.count() == 0) {
                                    return false
                                }
                                echo "Detected new builds created by buildconfig: ${it.names()}"
                                return true
                            }
                        }
                    }

                    timeout(10) {
                        builds.untilEach(1) {
                            return it.object().status.phase == "Complete"
                        }
                    }

                    return new HashMap([names: newBuild.names(), buildConfigName: buildConfigName])
                }
                finally {
                    if (newBuild) {
                        def result = newBuild.narrow("bc").logs()
                        echo "status: ${result.status}"
                        echo "${result.actions[0].cmd}"

                        if (result.status != 0) {
                            echo "${result.out}"
                            currentBuild.result = 'FAILURE'
                        }
                        if(deleteBuild) {
                            newBuild.delete()
                        }
                    }
                }
            }
        }
    }
}
