#!groovy

@Library('Utils')
import com.redhat.*


node {

    def utils = new Utils()

    stage('test') {

        String jsonString = """
        {
            "secret": "${secret}",
            "pid": "p17633880910e488f5949aab3ad76cd4317542a7a06"
        }
        """


        println(jsonString)

        def results = utils.postUrl("https://connect.redhat.com/api/container/status", jsonString, true)
        println(results)

    }



}