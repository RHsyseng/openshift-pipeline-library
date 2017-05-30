#!groovy

pipelineJob('CheckHealthGrade') {
    definition {
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
    parameters {
        stringParam('JOBNAME', 'foo', 'Name of rebuild job')
    }
    triggers {
        cron('@daily')
    }
}