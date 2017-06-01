#!groovy

pipelineJob('CheckHealthGrade') {
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        name('openshift-pipeline-library')
                        url('https://github.com/RHsyseng/openshift-pipeline-library')
                    }
                    branch('master')
                }
            }
            scriptPath('examples/containerzone/generic/jobs/CheckHealthGrade/checkHealthGradePipeline.groovy')
        }
    }
    parameters {
        stringParam('JOBNAME', 'foo', 'Name of rebuild job')
    }
    triggers {
        cron('@daily')
    }
}
