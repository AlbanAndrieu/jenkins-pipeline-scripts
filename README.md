<!-- markdown-link-check-disable-next-line -->
## [![Nabla](http://albandrieu.com/nabla/index/assets/nabla/nabla-4.png)](https://github.com/AlbanAndrieu) jenkins-pipeline-scripts

[![License: APACHE](http://img.shields.io/:license-apache-blue.svg?style=flat-square)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![License: MIT](https://img.shields.io/badge/license-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Gitter](https://badges.gitter.im/jenkins-pipeline-scripts/Lobby.svg)](https://gitter.im/jenkins-pipeline-scripts/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Join the chat at https://gitter.im/AlbanAndrieu/warnings-plugin](https://badges.gitter.im/AlbanAndrieu/warnings-plugin.svg)](https://gitter.im/AlbanAndrieu/warnings-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Minimal java version](https://img.shields.io/badge/java-1.8-yellow.svg)](https://img.shields.io/badge/java-1.8-yellow.svg)

[![Jenkins Version](https://img.shields.io/badge/Jenkins-2.221-green.svg?label=min.%20Jenkins)](https://jenkins.io/download/)
[![Build Status](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/actions/workflows/maven-build.yml/badge.svg)](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/actions/workflows/maven-build.yml)

[![Main Workflow](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/actions/workflows/maven-build.yml/badge.svg)](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/actions/workflows/maven-build.yml)
[![Docker Workflow](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/actions/workflows/docker-build.yml/badge.svg)](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/actions/workflows/docker-build.yml)
[![Trivy](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/actions/workflows/trivy.yml/badge.svg)](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/actions/workflows/trivy.yml)

[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/AlbanAndrieu/jenkins-pipeline-scripts?label=changelog)](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/releases)

[![Codecov](https://img.shields.io/codecov/c/github/AlbanAndrieu/jenkins-pipeline-scripts.svg)](https://codecov.io/gh/AlbanAndrieu/jenkins-pipeline-scripts)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/AlbanAndrieu/jenkins-pipeline-scripts.svg)](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/pulls)
[![Docker Pulls](https://img.shields.io/docker/pulls/nabla/jenkins-pipeline-scripts)](https://hub.docker.com/r/nabla/jenkins-pipeline-scripts)<br/>

# Table of contents

// spell-checker:disable

<!-- toc -->

- [Initialize](#initialize)
- [Usage](#usage)
- [Docker](#docker)
- [Kubernetes](#kubernetes)
- [Graph dependency](#graph-dependency)
  * [sonar](#sonar)
- [Folder Structure Conventions](#folder-structure-conventions)
  * [A typical top-level directory layout](#a-typical-top-level-directory-layout)
- [Update documentation](#update-documentation)
- [mega-linter](#mega-linter)
- [Check secret](#check-secret)
- [Update README.md](#update-readmemd)
  * [npm-groovy-lint groovy formating for Jenkinsfile](#npm-groovy-lint-groovy-formating-for-jenkinsfile)

<!-- tocstop -->

// spell-checker:enable

*NOTE: This repository is being deprecated internally at Nabla and hence
will receive few updates going forward.*

## Initialize

```bash
direnv allow
pyenv install 3.10.4
pyenv local 3.10.4
python -m pipenv install --dev --ignore-pipfile
direnv allow
pre-commit install
```

## Usage

This repository contains helper functions and classes to be used with the Jenkins Pipeline Plugin.
This repository is used on <http://albandrieu.com/jenkins/> and other Jenkins instances managed by Nabla.

Below feature have been removed on purpose
* [tee](https://jenkins.io/doc/pipeline/steps/pipeline-utility-steps/#-tee-%20tee%20output%20to%20file)

To use this library from your `Jenkinsfile`,
make sure you have installed the _GitHub Organization Folder_ in version 1.5 or later,
then start off with:

```groovy
@Library('jenkins-pipeline-scripts@master') _
```

OR (if jenkins is managing the version, the preferred way)

```groovy
@Library('jenkins-pipeline-scripts') _
```
In jenkins

Library

* Set Load implicitly to false
* Allow default version to be overridden to true
* Include @Library changes in job recent changes

Behaviour

Within Repository -> Discover branches
Additional -> Wipe out repository & force clone

See [Extending with Shared Libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) for more
information on Jenkins pipeline extensions.

See also [Best Practices For Pipeline Code](https://jenkins.io/blog/2017/02/01/pipeline-scalability-best-practice/)

Run test

```bash
./mvnw -Dtest=TestSharedLibrary test
```


## Docker

Build and Run

```bash
./scripts/docker-build.sh
```

or

```bash
$ docker build -t groovy-test .
#You can reproduce issue `Conflicting module versions. Module [groovy-all is loaded in version 2.4.8 and you are trying to load version 2.4.12` with
$ docker run -it groovy-test:latest
```

## Kubernetes

[cheatsheet](https://kubernetes.io/fr/docs/reference/kubectl/cheatsheet)
[conventions](https://helm.sh/docs/chart_best_practices/conventions/)

Install [microk8s](https://ubuntu.com/blog/deploying-kubernetes-locally-microk8s)

[Make](https://microk8s.io/docs/registry-images) docker image available to microk8s

```bash
$docker save nabla/jenkins-pipeline-scripts:1.0.3 > jenkins.tar
$microk8s ctr image import jenkins.tar

$microk8s ctr images ls
```


Create jenkins namespace

```bash
$k apply -f jenkins-namespace.yaml
```

Add [deployment](https://kubernetes.io/fr/docs/concepts/workloads/controllers/deployment/)

```bash
k config get-contexts
k config use-context microk8s
```

```bash
$ #k delete pods --all
#k delete -f jenkins-deployment.yaml
$k apply -f jenkins-deployment-local.yaml -n jenkins

$k get deployments jenkins-master -n jenkins --watch
$k describe pod -n jenkins | grep jenkins
#stop deployement
$k scale --replicas=0 deployment/jenkins-master -n jenkins
```

Copy volume data

```bash
cp -r /jenkins/* /mnt/jenkins
chown -R albandrieu:docker /mnt/jenkins
```

Add service

```bash
$k create -f jenkins-service.yaml -n jenkins
$k get service -n jenkins
$k logs jenkins-master-7b49df974d-kzlrg -n jenkins

```

Check <http://127.0.0.1:32082/>

Check [nfs](https://github.com/kubernetes/examples/tree/master/staging/volumes/nfs)

Add [PersistentVolume](https://kubernetes.io/docs/tasks/configure-pod-container/configure-persistent-volume-storage/#create-a-persistentvolume)


```bash
$k create -f jenkins-pvc.yaml -n jenkins
$k get pvc pvc-jenkins-home -n jenkins
$k create -f jenkins-volume.yaml -n jenkins
$k get pv jenkins-pv-volume -n jenkins

#k describe pv  -n jenkins
```

```bash
$k exec -it jenkins-master-7b49df974d-kzlrg -n jenkins -- /bin/bash
```

[service-account-tokens](https://kubernetes.io/docs/reference/access-authn-authz/authentication/#service-account-tokens)

```bash
$k create serviceaccount jenkins-account -n jenkins
$k get serviceaccounts jenkins-account -o yaml  -n jenkins
$k get secret jenkins-token-2dmg9 -o yaml  -n jenkins
```

[set-up-jenkins-in-a-kubernetes-cluster](https://medium.com/swlh/set-up-jenkins-in-a-kubernetes-cluster-96660c8d9ab)

```bash
$k apply -f jenkins-resourcequota.yaml -n jenkins
$k apply -f jenkins-role.yaml -f jenkins-serviceaccount.yaml -f jenkins-rolebinding.yaml -n jenkins
$k apply -f jenkins-deployment.yaml -n jenkins

$k get pods -n jenkins
$k -n jenkins port-forward jenkins-master-7b49df974d-kzlrg 8080:8080

$k get svc -n jenkins
```

See [dns-debugging-resolution](https://kubernetes.io/docs/tasks/administer-cluster/dns-debugging-resolution/)

```bash
$k get pods --namespace=kube-system -l k8s-app=kube-dns

$k -n kube-system describe configmap/coredns
$#Add 10.21.200.3 10.25.200.3 before google DNS
$k -n kube-system edit configmap coredns
```

Add k8s jenkins-account to jenkins
[set-up-jenkins-in-a-kubernetes-cluster](https://medium.com/swlh/quick-and-simple-how-to-setup-jenkins-distributed-master-slave-build-on-kubernetes-37f3d76aae7d)


```bash
$kubectl get secret $(kubectl get sa jenkins-account -n jenkins -o jsonpath={.secrets[0].name}) -n jenkins -o jsonpath={.data.token} | base64 --decode
$kubectl config view --minify | grep server | cut -f 2- -d ":" | tr -d " "
$kubectl get secret $(kubectl get sa jenkins-account -n jenkins -o jsonpath={.secrets[0].name}) -n jenkins -o jsonpath={.data.'ca\.crt'} | base64 --decode
```

Add k8s proxy


```bash
k get pod -n jenkins
k port-forward -n jenkins jenkins-master-6868bb694-m4jhb 8080:8080
http://localhost:8080/
```

TODO : Have proper DNS service

<http://jenkins-master.jenkins.svc.cluster.local>
mon-service.mon-namespace.svc.cluster.local

========================

See [Extending environment variables with Shared Libraries](https://devops.datenkollektiv.de/programatically-add-environment-variables-to-a-jenkins-instance.html)

## Graph dependency

* [graphviz](https://www.graphviz.org/pdf/dotguide.pdf)
* [webgraphviz](http://webgraphviz.com/)

```bash
dot -Tps draftStage.gv -o draftStage.ps
dot -Tpng draftStage.gv -o draftStage.png
dot -Tsvg draftStage.gv -o draftStage.svg
```

<img src="draftStage.png" width="1200" height="800" />

```bash
terraform graph | grep -v -e 'meta' -e 'close' -e 's3' -e 'vpc' -e 'expand' | dot -Tpng > terraform.png
```

<img src="terraform.png" width="1200" height="800" />

![pods-helm-sample](pods-helm-sample.svg)

#### maven

Maven `mvn clean deploy` will be started by default if `pom.xml` file exists and a sonar scan will be started using [sonar-maven-plugin](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-maven/)...


### sonar

Sonar will be started by default on maven projects.

[sonar-maven-plugin](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-maven/). is the preferred way to run sonar. Otherwise [sonarscanner](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner/) can be used.

If `sonar-project.properties` file exists a sonarscanner can be started...
sonarscanner is the preferred way for C++/ObjectiveC and Python projects.

## Folder Structure Conventions

> Folder structure options and naming conventions for software projects

### A typical top-level directory layout

    .
    ├── docs                    # Documentation files (alternatively `doc`)
    docker                      # Deprectated, using packs/Dockerfile instead
    docker-compose              # Put docker-compose files
    ├── src                     # Source files (alternatively `lib` or `app`)
    ├── resources               # Resources for jenkins
    ├── vars                    # Groovy scripts for jenkins
    bower.json                  # Bower not build directly, using maven instead
    Dockerfile                  # Deprectated, using packs/Dockerfile instead
    Jenkinsfile
    Jenkinsfile-checkmarx       # Will run Checkmarx scan
    Jenkinsfile-aqua            # Will run WhiteSource. Aqua as standalone scan
    package.json                # Nnpm not build directly, using maven instead
    pom.xml                     # Will run maven clean install
    .pre-commit-config.yaml
    requirements.testing.txt    # Python package used for test and build only
    requirements.txt            # Python package used for production only
    tox.ini
    sonar-project.properties    # Will run sonar standalone scan
    LICENSE
    CHANGELOG.md
    README.md
    ├── target                  # Compiled files (alternatively `dist`) for maven
    └── test                    # Automated tests

    .
    ├── ...
    ├── test                    # Test files
    │   ├── e2e                 # End-to-end, integration tests (alternatively `e2e`)
    │   karma.conf.js
    │   ├── postman             # API tests for postman
    │   protractor.conf.js
    │   └── spec                # Karma unit tests
    └── ...

    docker-compose irectory is used only to test project in jenkins
    .
    ├── ...
    ├── docker-compose          # Docker compose files
    │   docker-compose.yml
    │   docker-compose.dev.yml  # For developper (with port open)
    │   docker-compose.prod.yml # For production such as jenkins
    │   docker-compose.test.yml # For tests such as newman, robot
    └── ...

    docker irectory is used only to build project
    .
    ├── ...
    ├── docker                  # Docker files used to build project
    │   ├── centos7             # End-to-end, integration tests (alternatively `e2e`)
    │   ├── ubuntu18            # End-to-end, integration tests (alternatively `e2e`)
    │   └── ubuntu20
    │       Dockerfile          # File to build
    │       config.yaml         # File to run CST
    └── ...

    .
    ├── ...
    ├── docs                    # Documentation files
    │   ├── index.rst           # Table of contents
    │   ├── faq.rst             # Frequently asked questions
    │   ├── misc.rst            # Miscellaneous information
    │   ├── usage.rst           # Getting started guide
    │   └── ...                 # etc.
    └── ...

    .
    ├── ...
    ├── packs                    # Files used to build docker image and chart
    │   config.yaml              # File to run CST
    │   Dockerfile               # File to build docker image
    │   └── newman               # Name of the helm chart
    │       └── charts
    │           Chart.yaml
    │           README.md
    │           └── templates
    │               deployment.yaml
    │               └── tests
    │                   test-connection.yaml
    │           values.yaml
    └── ...


## Update documentation

```bash
mvn gplus:groovydoc
# or
mvn site
```

Maven site and groovy doc will be published with jenkins build

`README.md` then `CHANGELOG.md` are the default entry points.

## mega-linter

```bash
npx mega-linter-runner
```

## Check secret

```
npx @secretlint/quick-start "**/*"
```

## Update README.md


* [github-markdown-toc](https://github.com/jonschlinkert/markdown-toc)
* With [github-markdown-toc](https://github.com/Lucas-C/pre-commit-hooks-nodejs)

```bash
npm install --save markdown-toc
markdown-toc README.md
markdown-toc CHANGELOG.md  -i
```

```bash
pre-commit install
git add README.md
pre-commit run markdown-toc
```

### npm-groovy-lint groovy formating for Jenkinsfile

Tested with nodejs 12 and 16 on ubuntu 20 and 21 (not working with nodejs 11 and 16)

```
npm install -g npm-groovy-lint@8.2.0
npm-groovy-lint --format
ls -lrta .groovylintrc.json
```
