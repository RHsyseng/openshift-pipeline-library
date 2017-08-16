#!groovy
import groovy.json.JsonSlurperClassic

import java.util.UUID

def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def newBuild = null
    def buildConfig = null
    def builds = null
    def newBuildObjectNames = null
    String contextDir = config['contextDir'] ?: ""
    String image = ""
    String name = config['name'] ?: ""
    String imageStream = ""
    String baseImageStreamName = ""
    String strategy = ""

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

    stage("${name} - OpenShift Build") {
        openshift.withCluster() {
            openshift.withProject() {
                Boolean buildConfigExists = false

                try {
                    buildConfig = openshift.selector('buildconfig', name)
                    buildConfigExists = buildConfig.exists()
                }
                catch (err) {
                    // This resolves an error that will occur if using 3.4.x oc binary
                    buildConfigExists = false
                }


                try {
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

                        if(buildConfigExists) {
                            /* With the 3.4 oc binary this will not function */
                            buildConfig.startBuild()
                        }
                        else {
                            newBuildRaw = openshift.raw("-o json",
                                    "new-build", "${image}${config.url}#${config.branch}",
                                    "--name=${name}",
                                    "--context-dir=${contextDir}",
                                    "${imageStream}", "--dry-run")

                            baseImageStreamName = findBaseImageStream((String)(name), (String)(newBuildRaw.out))

                            if (openshift.selector('is', baseImageStreamName).exists()) {
                                println("openshift.project()")
                                image = "${openshift.project()}/${baseImageStreamName}~"
                                strategy = "--strategy=docker"
                            }

                            newBuild = openshift.newBuild("${image}${config.url}#${config.branch}",
                                    "--name=${name}",
                                    "--context-dir=${contextDir}",
                                    "${imageStream}",
                                    "${strategy}")
                            echo "newBuild created: ${newBuild.count()} objects : ${newBuild.names()}"

                            buildConfig = newBuild.narrow("bc")
                        }
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
                            switch (it.object().status.phase) {
                                case "Complete":
                                    return true
                                case "Error":
                                    println("message: ${it.object().status.message}")
                                    currentBuild.result = 'FAILURE'
                                    return true
                                case "Failed":
                                    println("message: ${it.object().status.message}")
                                    currentBuild.result = 'FAILURE'
                                    return true
                                default:
                                    return false
                            }
                        }
                    }


                    if (newBuild) {
                        newBuildObjectNames = newBuild.names()
                    } else {
                        newBuildObjectNames = ["bc/${buildConfigName}",
                                               "is/${baseImageStreamName}",
                                               "is/${buildConfigName}"]
                    }

                    return new HashMap([names: newBuildObjectNames, buildConfigName: buildConfigName])
                }
                catch(all) {
                    println(all)
                }
                finally {
                    if (buildConfig) {
                        def result = buildConfig.logs()

                        if(deleteBuild) {
                            newBuild.delete()
                        }
                        // After the build is complete clean up the builds
                        builds.delete()
                    }
                }
            }
        }
    }
}

@NonCPS
String findBaseImageStream(String name, String newBuildRawOutput) {
    String baseImageStreamName = ""
    JsonSlurperClassic parser = new JsonSlurperClassic()

    HashMap newBuildMap = (HashMap) parser.parseText(newBuildRawOutput)
    parser = null

    for (item in newBuildMap["items"]) {
        if( item.kind == 'ImageStream') {
            if (item["metadata"]["name"] != name) {
                baseImageStreamName = item["metadata"]["name"]
            }
        }
    }
    return baseImageStreamName
}
