#### Using Jenkins to test playbook2image


```
oc new-project p2i-ci 

# Retrieve token from the url below
# https://github.com/settings/tokens/new?scopes=repo,read:user,user:email

oc secrets new-basicauth github --username= --password=

oc new-build openshift/jenkins~https://github.com/aweiteka/playbook2image --context-dir='jenkins' --name='jenkins'
oc process openshift//jenkins-ephemeral NAMESPACE=p2i-ci MEMORY_LIMIT=2Gi | oc create -f -


oc create -f jenkins/openshift/pipeline.yaml
```

