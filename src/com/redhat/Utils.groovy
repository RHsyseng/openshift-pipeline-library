#!groovy
package com.redhat

//@Grab('org.kohsuke:github-api:1.85')

@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.5.2')

import com.cloudbees.groovy.cps.NonCPS
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*


import org.apache.http.protocol.*
import org.apache.http.conn.*
import org.apache.http.client.*
import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*
import org.apache.http.client.config.*

import jenkins.model.*
import jenkins.model.Jenkins
import hudson.security.*
import jenkins.model.JenkinsLocationConfiguration
import org.kohsuke.github.*
import hudson.model.*
import groovy.json.*

import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.SSLException


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

/**
 * This method POSTs to a HTTP uri with the requestJsonString as the payload.
 * The retry boolean is available for HTTP-based APIs that incorrectly implement
 * the POST method.  This will force a retry if a POST is used in place of the GET
 * HTTP method.  ** Using retry should be avoided. **
 *
 * http://restcookbook.com/HTTP%20Methods/idempotency/
 * https://hc.apache.org/httpcomponents-client-ga/tutorial/html/fundamentals.html
 * @param uri
 * @param requestJsonString
 * @param retry
 * @return
 */
static final HashMap postUrl(String uri, String requestJsonString, boolean retry = false) {

    CloseableHttpResponse response
    CloseableHttpClient client
    int timeout = 3;
    int socketTimeout = 30;
    RequestConfig config = RequestConfig.custom().
            setConnectTimeout(timeout * 1000).
            setConnectionRequestTimeout(timeout * 1000).
            setSocketTimeout(socketTimeout * 1000).build();

    if (retry) {
        HttpRequestRetryHandler postRequestHandler = new HttpRequestRetryHandler() {

            public boolean retryRequest(IOException exception,
                                        int executionCount,
                                        HttpContext context) {
                if (executionCount >= 5) {
                    return false
                }
                if (exception instanceof InterruptedException) {
                    // timeout
                    return false
                }
                if (exception instanceof UnknownHostException) {
                    // Unknown host
                    return false
                }
                if (exception instanceof ConnectTimeoutException) {
                    // Connection refused
                    return false
                }
                if (exception instanceof SSLException) {
                    // SSL handshake exception
                    return false
                }
                return true
            }
        }

        client = HttpClientBuilder.create()
                .setRetryHandler(postRequestHandler)
                .setDefaultRequestConfig(config).build()
    }
    else {
        client = HttpClientBuilder.create()
                .setDefaultRequestConfig(config).build();
    }

    HttpPost httpPost = new HttpPost(uri)

    httpPost.addHeader("content-type", "application/json")
    HashMap resultMap = new HashMap()

    try {
        httpPost.setEntity(new StringEntity(requestJsonString))
        response = client.execute(httpPost)

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
        String jsonResponse = bufferedReader.getText()
        // No longer need the reader or the response
        bufferedReader.close()
        response.close()
        /* The JsonSluperClassic must be used vs JsonSlurper
         * since it returns a LazyMap which is not serializable.
         */
        JsonSlurperClassic parser = new JsonSlurperClassic()
        resultMap = (HashMap) parser.parseText(jsonResponse)
        parser = null
        return resultMap
    }
    catch (IOException ioe) {
        Logger.getLogger("com.redhat.Utils").log(Level.SEVERE, ioe.toString())
        throw ioe
    }
    catch (ClientProtocolException cpe) {
        Logger.getLogger("com.redhat.Utils").log(Level.SEVERE, cpe.toString())
        throw cpe
    }
    catch (Exception e) {
        Logger.getLogger("com.redhat.Utils").log(Level.SEVERE, e.toString())
        throw e
    }
    finally {
        client.close()
    }
}


@NonCPS
def getDockerCfgPassword(String dockerCfg) {

    JsonSlurperClassic parser = new JsonSlurperClassic()
    HashMap dockerCfgMap = (HashMap)parser.parseText(new String(dockerCfg.decodeBase64()))
    parser = null

    Set keys = dockerCfgMap.keySet()
    Integer size = (Integer) keys.size()

    if(size != 1) {
        throw new Exception("dockerCfgMap keySet should only be a size of one (1) and is ${size}")
    }

    return dockerCfgMap[keys[0]].password
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
