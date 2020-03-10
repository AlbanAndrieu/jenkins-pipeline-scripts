jenkins-pipeline-scripts
========================

[![Join the chat at https://gitter.im/AlbanAndrieu/warnings-plugin](https://badges.gitter.im/AlbanAndrieu/warnings-plugin.svg)](https://gitter.im/AlbanAndrieu/warnings-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Jenkins Version](https://img.shields.io/badge/Jenkins-2.221-green.svg?label=min.%20Jenkins)](https://jenkins.io/download/)
![JDK8](https://img.shields.io/badge/jdk-8-yellow.svg?label=min.%20JDK)
[![License: MIT](https://img.shields.io/badge/license-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/AlbanAndrieu/jenkins-pipeline-scripts?label=changelog)](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/releases)

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

Run docker

```
$ docker build -t groovy-test .
#You can reproduce issue `Conflicting module versions. Module [groovy-all is loaded in version 2.4.8 and you are trying to load version 2.4.12` with 
$ docker run -it groovy-test:latest
```

========================

See [Extending environment variables with Shared Libraries](https://devops.datenkollektiv.de/programatically-add-environment-variables-to-a-jenkins-instance.html)

[![GitHub Actions](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/workflows/GitHub%20Actions/badge.svg)](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/actions)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/1999b59401394431a1c2fea2923a919d)](https://www.codacy.com/app/uhafner/jenkins-pipeline-scripts?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=AlbanAndrieu/jenkins-pipeline-scripts&amp;utm_campaign=Badge_Grade)
[![Codecov](https://img.shields.io/codecov/c/github/AlbanAndrieu/jenkins-pipeline-scripts.svg)](https://codecov.io/gh/AlbanAndrieu/jenkins-pipeline-scripts)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/AlbanAndrieu/jenkins-pipeline-scripts.svg)](https://github.com/AlbanAndrieu/jenkins-pipeline-scripts/pulls)
