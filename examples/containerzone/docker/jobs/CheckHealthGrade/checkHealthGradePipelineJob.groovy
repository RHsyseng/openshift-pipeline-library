#!groovy

pipelineJob('CheckHealthGrade') {
    defintion {
        cpsScm {
            scm {
                git {
                    remote {
                        name('openshift-pipeline-library')
                        url('https://github.com/jcpowermac/openshift-pipeline-library')
                    }
                    branch('updates')
                }
            }
            scriptPath('examples/containerzone/docker/jobs/CheckHealthGrade/checkHealthGradePipeline.groovy')
        }
    }

    triggers {
        cron('@daily')
    }
}
