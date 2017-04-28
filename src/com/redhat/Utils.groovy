#!groovy
package com.redhat

//@Grab('org.kohsuke:github-api:1.85')

import com.cloudbees.groovy.cps.NonCPS
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import java.util.logging.Level
import java.util.logging.Logger
import jenkins.model.JenkinsLocationConfiguration
import org.kohsuke.github.*
import hudson.model.*


/**
 * This function just removes the limitation of using the spread operator
 * under CPS.  Maybe the plugin could also resolve this?
 * @param openshift
 * @param args
 */

@NonCPS
static void openShiftRun(def openshift, def args) {
    openshift.run(*args)
}

@NonCPS
List<hudson.model.ParameterValue> createJobParameters(HashMap configMap) {
    try {
        List<hudson.model.ParameterValue> parameters = new ArrayList<hudson.model.ParameterValue>()
        configMap.each{ k, v -> 
            parameters.add( new StringParameterValue("${k}", "${v}") )  
        }
        return parameters
    }
    catch(all) {
        Logger.getLogger("com.redhat.Utils").log(Level.SEVERE, all.toString())
        throw all
    }
}

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

@NonCPS
Boolean configureRootUrl(String url) {
    try {
        JenkinsLocationConfiguration jlc = JenkinsLocationConfiguration.get()
        jlc.setUrl(url)
        jlc.save()
        return true
    }
    catch (all) {
        Logger.getLogger("com.redhat.Utils").log(Level.SEVERE, all.toString())
        return false
    }
}

@NonCPS 
String createCredentialsFromOpenShift(HashMap secret, String id) {
    try {
        String username = new String(secret.data.username.decodeBase64())
        String password = new String(secret.data.password.decodeBase64())
        return createCredentials(id, username, password, "secret from openshift")
    }
    catch(all) {
        Logger.getLogger("com.redhat.Utils").log(Level.SEVERE, all.toString())
        throw all
    }
}

@NonCPS
String createCredentials(String id = null, String username, String password, String description) {
    try {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
        }
        Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, description, username, password)
        SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
        return id
    }
    catch (all) {
        Logger.getLogger("com.redhat.Utils").log(Level.SEVERE, all.toString())
        throw all
    }
}
