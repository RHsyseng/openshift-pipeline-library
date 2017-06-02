#!groovy
package com.redhat

import com.cloudbees.groovy.cps.NonCPS
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import groovy.json.JsonSlurperClassic
import jenkins.model.*
import jenkins.model.Jenkins
import hudson.security.*
import jenkins.model.JenkinsLocationConfiguration
import hudson.model.*

import java.util.logging.Level
import java.util.logging.Logger


@NonCPS
List<hudson.model.ParameterValue> createJobParameters(HashMap configMap) {
    try {
        List<hudson.model.ParameterValue> parameters = new ArrayList<hudson.model.ParameterValue>()
        configMap.each{ k, v ->
            parameters.add(new StringParameterValue("${k}", "${v}"))
        }
        return parameters
    }
    catch(all) {
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
String createCredentialsFromOpenShiftDockerCfg(HashMap secret, String id) {
    try {
        JsonSlurperClassic parser = new JsonSlurperClassic()
        String decoded = new String(secret['data']['.dockercfg'].decodeBase64())
        HashMap extractedMap = ((HashMap) parser
                .parseText(decoded)
                .entrySet()
                .iterator()
                .next()
                .getValue())

        parser = null
        decoded = null

        return createCredentials(id, (String) extractedMap.username, (String) extractedMap.password,
                "DockerCfg from OpenShift")

    }
    catch (all) {
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

@NonCPS
Boolean setAnonPermBuildStatusIcon() {

    def permissions = ["hudson.model.Item.ViewStatus", "hudson.model.View.Read"]
    def sid = "anonymous"

    return setJenkinsPermissions(permissions, sid)
}

//https://wiki.jenkins-ci.org/display/JENKINS/Grant+Cancel+Permission+for+user+and+group+that+have+Build+permission

@NonCPS
Boolean setJenkinsPermissions(def perms, def sid) {
    try {
        perms.each {
            Jenkins.instance.authorizationStrategy.add(Permission.fromId(it), sid)
        }
        Jenkins.instance.save()
        return true
    }
    catch (all) {
        Logger.getLogger("com.redhat.Utils").log(Level.SEVERE, all.toString())
        return false
    }
}
