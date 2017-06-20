### Pipeline Library Testing

#### tests/jobs

##### Mock Pipelines

- mockHealthCheck.groovy - Tests the vars/containerZoneHealthCheck.groovy function
- mockPipeline.groovy - Tests all the vars/containerZone.groovy functions
- mockPublish.groovy - Tests the vars/containerZonePublish.groovy function
- mockRebuild.groovy - Job to be called by mockHealthCheck
- mockScan.groovy - Tests the vars/containerZoneScan.groovy function

#### tests/jobs/__files/

`test.py` - This python script uses `tests.yml` to iterate through the mock pipelines to test each var function.
##### requirements

- Jenkins
    - pipeline job named `mock` to copy and replace the `scriptPath`
    - Username/password credential with the id of `ContainerZone`
        - The `username` will be the Container Zone project id (pid)
        - The `password` will be the Container Zone project `secret`
    - Required plugins see [plugins.txt](../jenkins/plugins.txt)
    - This global pipeline library configured with the name `Utils`
- OpenShift Project
    - [openshift-wiremock project](https://github.com/jcpowermac/openshift-wiremock)
    - OpenShift generic secret named `containerzone` with one parameter named `secret`
- Local
    - `pip install python-jenkins wiremock PyYAML`
