#!groovy

package com.redhat

import org.kohsuke.github.*
import java.util.logging.Level
import java.util.logging.Logger


@NonCPS
HashMap getGitHubPR(String login, String oauthAccessToken, String changeUrl) {
    try {
        String[] changeUrlArray = changeUrl.split('[/]')
        String organization = changeUrlArray[3]
        String repository = changeUrlArray[4]
        int pullRequest = Integer.parseInt(changeUrlArray[6])

        return getGitHubPR(login, oauthAccessToken, organization, repository, pullRequest)
    }
    catch(all){
        Logger.getLogger("com.redhat.Utils").log(Level.SEVERE, all.toString())
        throw all
    }
}

@NonCPS
HashMap getGitHubPR(String login, String oauthAccessToken, String organization, String repository, int pullRequest) {
    HashMap map = [:]
    try {
        GitHub github = GitHub.connect(login, oauthAccessToken)

        GHCommitPointer pointer = github.getRepository("${organization}/${repository}")
                .getPullRequest(pullRequest).getHead()

        map['ref'] = pointer.ref
        map['url'] = pointer.repository.gitHttpTransportUrl().toString()
        return map
    }
    catch (all) {
        Logger.getLogger("com.redhat.Utils").log(Level.SEVERE, all.toString())
        throw all
    }
}
