#!groovy
@Library('Utils')
import com.redhat.*

properties([disableConcurrentBuilds()])

node {
    def source = ""
    def dockerfiles = null
    def gitHubUtils = new com.redhat.GitHubUtils()
    String scmRef = scm.branches[0]
    String scmUrl = scm.browser.url

    /* Checkout source and find all the Dockerfiles.
     * This will not include Dockerfiles with extensions. Currently the issue
     * with using a Dockerfile with an extension is the oc new-build command
     * does not offer an option to provide the dockerfilePath.
     */
    stage('checkout') {
        checkout scm
        dockerfiles = findFiles(glob: '**/Dockerfile')
    }


    /* if CHANGE_URL is defined then this is a pull request
     * additional steps are required to determine the git url
     * and branch name to pass to new-build.
     * Otherwise just use the scm.browser.url and scm.branches[0]
     * for new-build.
     */
    if (env.CHANGE_URL) {
        def pull = null
        stage('Github Url and Ref') {
            // Query the github repo api to return the clone_url and the ref (branch name)
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "github", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                pull = gitHubUtils.getGitHubPR(env.USERNAME, env.PASSWORD, env.CHANGE_URL)
                scmUrl = pull.url
                scmRef = pull.ref
                deleteBuild = true
            }
        }
    }
    for (int i = 0; i < dockerfiles.size(); i++) {

        def resources = null
        try {
        /* Execute oc new-build on each dockerfile available
         * in the repo.  The context-dir is the path removing the
         * name (i.e. Dockerfile)
         */
            def is = ""
            def dockerImageRepository = ""
            String path = dockerfiles[i].path.replace(dockerfiles[i].name, "")
            newBuild = newBuildOpenShift() {
                url = scmUrl
                branch = scmRef
                contextDir = path
                deleteBuild = false 
                randomName = true 
            }
            dockerImageRepository = getImageStreamRepo(newBuild.buildConfigName).dockerImageRepository

            runOpenShift {
                deletePod = true
                branch = scmRef
                image = dockerImageRepository
                env = ["foo=goo"]
            }

            resources = newBuild.names
            currentBuild.result = 'SUCCESS'
        }
        catch(all) {
            currentBuild.result = 'FAILURE'
            echo "Exception: ${all}"
        }
        finally {
            stage('Clean Up Resources') {
               openshift.withCluster() {
                    openshift.withProject() {
                        for (r in resources) {
                            openshift.selector(r).delete()
                        }
                    }
                }
            }
        }
    }
}

// vim: ft=groovy
