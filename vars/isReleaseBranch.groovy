
def call() {
    BRANCH_NAME ==~ /develop/ || BRANCH_NAME ==~ /master/ ||  BRANCH_NAME ==~ /release\/.*/
}
