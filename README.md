jenkins-pipeline-scripts
========================

*NOTE: This repository is being deprecated internally at Nabla and hence
will receive few updates going forward.*

This repository contains helper functions and classes to be used with the Jenkins Pipeline Plugin.
This repository is used on http://home.nabla.mobi:8381/jenkins/ and other Jenkins instances managed by Nabla.

Below feature have been removed on purpose
 * [tee](https://jenkins.io/doc/pipeline/steps/pipeline-utility-steps/#-tee-%20tee%20output%20to%20file)

To use this library from your `Jenkinsfile`,
make sure you have installed the _GitHub Organization Folder_ in version 1.5 or later,
then start off with:

```groovy
@Library('jenkins-pipeline-scripts@develop') _
```

OR (if jenkins is managing the version, the prefered way)

```groovy
@Library('jenkins-pipeline-scripts') _
```

See [Extending with Shared Libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) for more
information on Jenkins pipeline extensions.

See also [Best Practices For Pipeline Code](https://jenkins.io/blog/2017/02/01/pipeline-scalability-best-practice/)

Run test

```
./mvnw -Dtest=TestSharedLibrary test
```

========================

See [Extending environment variables with Shared Libraries](https://devops.datenkollektiv.de/programatically-add-environment-variables-to-a-jenkins-instance.html) 
