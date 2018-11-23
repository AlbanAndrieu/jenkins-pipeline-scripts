#!/usr/bin/groovy

def call() {
    env.BRANCH_NAME ==~ /develop|master|master_.+|release\/.+/
}
