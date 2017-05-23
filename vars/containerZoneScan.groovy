#!groovy

import com.redhat.connect.ContainerZone

def call(String dockerCfg, String dockerDigest) {
    def containerZone = new com.redhat.connect.ContainerZone(dockerCfg)
    containerZone.setDockerImageDigest(dockerDigest)

    stage('Scanning') {
      containerZone.waitForScan(20, 30)
    }
    stage('Scan Results') {
      def scanResults = containerZone.getScanResults()
      wrap([$class: 'AnsiColorBuildWrapper']) {
          print(scanResults.output)
      }
      if( !(scanResults.success) ) {
          error("Certification Scan Failed")
      }
    }
}



/**
 * Which constructor should we call
 * TODO: determine a better way to do this...
 *
 */

def call(Map parameters = [:] ) {

    def containerZone = null
    if( parameters.containsKey("dockerCfg") ) {
        containerZone = new com.redhat.connect.ContainerZone(parameters.dockerCfg)
        containerZone.setDockerImageDigest(parameters.dockerDigest)
    }
    else {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${parameters.credentialsId}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            containerZone = new com.redhat.connect.ContainerZone("${env.USERNAME}", "${env.PASSWORD}", parameters.dockerDigest)
        }
    }

    stage('Scanning') {
      containerZone.waitForScan(20, 30)
    }
    stage('Scan Results') {
      def scanResults = containerZone.getScanResults()
      wrap([$class: 'AnsiColorBuildWrapper']) {
          print(scanResults.output)
      }
      if( !(scanResults.success) ) {
          error("Certification Scan Failed")
      }
    }




}
