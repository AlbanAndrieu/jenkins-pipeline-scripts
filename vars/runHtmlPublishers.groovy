#!/usr/bin/groovy

import hudson.model.*
import com.cloudbees.groovy.cps.NonCPS
// Method to run all common Jenkins HTML Publishers (Maven, Java, C++, ALMTest) in single tasks

// Each Publisher task should be be described as map of {name, config}, where
// name:   is the name of publisher as defined in Jenkins
// config: a map of parameters in the same format, as Jenkins configuration, it should be used to override defaults.
//         for example, parsingRulesPath can be overridden from calling script

// Usage:
// 1. run default publishers:
// runHtmlPublishers()
//
// 2. run publishers with default config
// runHtmlPublishers(["LogParserPublisher"])
//
// 3. run publishers with updated config
//     LogParserPublisher: [parsingRulesPath: "/custom/rules/path"]
// ])

class PublisherDefaults {

    static Map LogParserPublisher = [
        $class: 'LogParserPublisher',
        parsingRulesPath:
        '/jenkins/deploy-log_parsing_rules',
        failBuildOnError: false,
        unstableOnWarning: false,
        useProjectRule: false
    ]

    // Below Publisher has been deprecated and must be removed
    static Map AnalysisPublisher = [
        $class: "AnalysisPublisher",
        canComputeNew: false,
        checkStyleActivated: false,
        defaultEncoding: '',
        dryActivated: false,
        findBugsActivated: false,
        healthy: '',
        opentasksActivated: false,
        pmdActivated: false,
        unHealthy: ''
    ]

    // Below Publisher has been deprecated and must be removed
    static Map WarningsPublisher = [
        $class: "WarningsPublisher",
        canComputeNew: false,
        canResolveRelativePaths: false,
        canRunOnFailed: true,
        consoleParsers: [
            [
                parserName: 'Java Compiler (javac)'
            ],
            [
                parserName: 'Maven'
            ],
            [
                parserName: 'GNU Make + GNU C Compiler (gcc)', pattern: 'error_and_warnings.txt'
            ],
            [
                parserName: 'Clang (LLVM based)', pattern: 'error_and_warnings_clang.txt'
            ]
        ],
        usePreviousBuildAsReference: true,
        useStableBuildAsReference: true
    ]

    static Map ALMTestPublisher = [
        allowMissing: false,
        alwaysLinkToLastBuild: false,
        keepAll: true,
        reportDir: "./latestResult/",
        reportFiles: 'index.html',
        reportName: 'AlmondeTest Report',
        reportTitles: 'ALMTEST index'
    ]

    static Map RobotPublisher = [
        $class: 'RobotPublisher',
        outputPath: "/home/frrobot/results",
        outputFileName: "output.xml",
        reportFileName: 'report.html',
        logFileName: 'log.html',
        disableArchiveOutput: false,
        passThreshold: 100.0,
        unstableThreshold: 80.0,
        otherFiles: "*.png,*.jpg",
    ]

}

// Shortcut for creating default publishers (with no flags)
def call() {
    Map defaultPublishers = [
        LogParserPublisher: [:],
        AnalysisPublisher: [:]
    ]
    call(defaultPublishers)
}

// Shortcut for creating publishers from a list
@NonCPS
def call(List<String> publishers) {
    Map defaultPublishers = [:]
    publishers.each() { defaultPublishers[it] = [:] }
    call(defaultPublishers)
}

@NonCPS
def call(Map publishers) {

    publishers.each { publisherName, publisherConfig ->
        echo "[JPL] Running ${publisherName} with configuration: ${publisherConfig.toString()}"
        switch (publisherName) {
            case "LogParserPublisher":
                Map LogParserPublisherConfig = PublisherDefaults.LogParserPublisher << publisherConfig
                step(LogParserPublisherConfig)
                break
            //case "AnalysisPublisher":
            //    Map AnalysisPublisherConfig = PublisherDefaults.AnalysisPublisher << publisherConfig
            //    step(AnalysisPublisherConfig)
            //    break
            //case "WarningsPublisher":
            //    Map WarningsPublisherConfig = PublisherDefaults.WarningsPublisher << publisherConfig
            //    step(WarningsPublisherConfig)
            //    break
            case "ALMTestPublisher":
                Map ALMTestPublisherConfig = PublisherDefaults.ALMTestPublisher << publisherConfig
                publishHTML(ALMTestPublisherConfig)
                break
            case "RobotPublisher":
                Map RobotPublisherConfig = PublisherDefaults.RobotPublisher << publisherConfig
                step(RobotPublisherConfig)
                break
            default:
                echo "[JPL] Unknown results Publisher"
                break
        }
    }
    echo "[JPL] Finished running HTML Publishers"
}
