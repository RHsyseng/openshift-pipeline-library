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
import hudson.security.*
import hudson.model.*
import groovy.json.*

import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.SSLException


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
        // Logger.getLogger("com.redhat.Utils").log(Level.INFO, "requestJsonString: ${requestJsonString}")
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
        // Logger.getLogger("com.redhat.Utils").log(Level.INFO, "jsonResponse: ${jsonResponse}")
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
