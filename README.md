jenkins-pipeline-scripts
========================

*NOTE: This repository is being deprecated internally at Nabla and hence
will receive few updates going forward.*

This repository contains helper functions and classes to be used with the Jenkins Pipeline Plugin.
This repository is used on http://home.nabla.mobi:8381/jenkins/ and other Jenkins instances managed by Nabla.

To use this library from your `Jenkinsfile`,
make sure you have installed the _GitHub Organization Folder_ in version 1.5 or later,
then start off with:

```groovy
@Library('nabla-pipeline-scripts@develop') _
```

OR (if jenkins is managing the version, the prefered way)

```groovy
@Library('nabla-pipeline-scripts') _
```

See [Extending with Shared Libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) for more
information on Jenkins pipeline extensions.
