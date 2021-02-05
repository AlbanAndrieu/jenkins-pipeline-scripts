# jenkins-pipeline-scripts

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

# Table of contents

<!-- toc -->

- [Changelog](#changelog)
- [[Unreleased]](#unreleased)
- [[1.0.0] - 2020-09-11](#100---2020-09-11)
  * [Added](#added)
  * [Updated](#updated)
- [Usage](#usage)

<!-- tocstop -->

Changelog
---------

## [Unreleased]

## [1.0.0] - 2020-09-11

### Added
- Force unix build on windows (mingw)

### Updated
- dockerLint and dockerHadoLint output as json
- use withRegistryWrapper for draft

Usage
-----

`
#!/usr/bin/env groovy
@Library(value='jenkins-pipeline-scripts', changelog=false) _
`
