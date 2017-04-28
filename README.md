### OpenShift Pipeline Library 

Current Goal: Build and test container images on OpenShift with Jenkins and supporting
multibranches (pull requests).

1. Uses ephermeral Jenkins and the configuration is stored in OpenShift project
2. S2I to include jobs and additional plugins

#### Quickstart

1. Add Jenkinsfile to your GitHub project (use the example in this project).
2. Create a new project `oc new-project <project-name>`
3. Add the template to OpenShift `oc create -f https://raw.githubusercontent.com/RHsyseng/openshift-pipeline-library/master/jenkins/openshift/template.yaml`
4. Process template
  ```
  oc new-app --template docker-image-testing \
  -p JENKINS_ORG_FOLDER_NAME=RHsyseng \
  -p JENKINS_GITHUB_OWNER=RHsyseng-user \
  -p JENKINS_GITHUB_REPO=openshift-client-library \
  -p JENKINS_GITHUB_CRED_ID=github \
  -p GITHUB_USERNAME=RHsyseng-user \
  -p GITHUB_TOKEN=token
  ```
5. Add Jenkins to the project `oc new-app --template jenkins-ephemeral -p NAMESPACE=$(oc project -q) -p MEMORY_LIMIT=2Gi`
6. Add the pipeline `oc create -f https://raw.githubusercontent.com/RHsyseng/openshift-pipeline-library/master/jenkins/openshift/pipeline.yaml`
7. And finally start the pipeline `oc start-build createcred-pipeline`


#### Note
This project is being tested by the `Jenkinsfile` in my OpenShift environment.
The SSL certificates are from Let's Encrypt using the [openshift-acme project](https://github.com/tnozicka/openshift-acme) to generate them.
