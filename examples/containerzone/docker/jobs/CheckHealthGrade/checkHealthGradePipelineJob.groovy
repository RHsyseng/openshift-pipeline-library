#!groovy

pipelineJob('CheckHealthGrade') {
    parameters {
        stringParam('JOBNAME', 'foo', 'Name of rebuild job')
    }
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
