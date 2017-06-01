#!groovy

organizationFolder("${name}") {
    description('This contains branch source jobs for GitHub')
    displayName("${displayName}")
    orphanedItemStrategy {
        discardOldItems {
            daysToKeep(0)
            numToKeep(0)
        }
    }
    organizations {
        github {
            apiUri('https://api.github.com')
            repoOwner("${repoOwner}")
            scanCredentialsId("${credentialsId}")
            pattern("${pattern}")
            checkoutCredentialsId("${credentialsId}")
            buildOriginBranch(true)
            buildOriginBranchWithPR(true)
            buildOriginPRMerge(false)
            buildOriginPRHead(false)
            buildForkPRMerge(true)
            buildForkPRHead(false)
        }
    }
    triggers {
        periodic(10)
    }
}
