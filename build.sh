#!/bin/bash
set -xv

#export bold="\033[01m"
#export underline="\033[04m"
#export blink="\033[05m"

#export black="\033[30m"
export red="\033[31m"
export green="\033[32m"
#export yellow="\033[33m"
#export blue="\033[34m"
#export magenta="\033[35m"
#export cyan="\033[36m"
#export ltgray="\033[37m"

export NC="\033[0m"

#export double_arrow='\xC2\xBB'
export head_skull='\xE2\x98\xA0'
export happy_smiley='\xE2\x98\xBA'
# shellcheck disable=SC2034
export reverse_exclamation='\u00A1'

# curl (REST API)
# Assuming "anonymous read access" has been enabled on your Jenkins instance.
# JENKINS_URL=[root URL of Jenkins master]
JENKINS_URL="https://home.nabla.mobi:8381/"
# JENKINS_CRUMB is needed if your Jenkins master has CRSF protection enabled as it should
JENKINS_CRUMB=`curl "$JENKINS_URL/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,\":\",//crumb)"`
curl -X POST -H $JENKINS_CRUMB -F "jenkinsfile=<Jenkinsfile" $JENKINS_URL/pipeline-model-converter/validate
RC=$?
if [ ${RC} -ne 0 ]; then
  echo ""
  # shellcheck disable=SC2154
  echo -e "${red} ${head_skull} Sorry, jenkins validation failed. ${NC}"
  exit 1
else
  echo -e "${green} The jenkins validation completed successfully. ${NC}"
fi

# ssh (Jenkins CLI)
# JENKINS_SSHD_PORT=[sshd port on master]
#JENKINS_SSHD_PORT=222
# JENKINS_HOSTNAME=[Jenkins master hostname]
#JENKINS_HOSTNAME=almonde-jenkins.misys.global.ad
#ssh -p $JENKINS_SSHD_PORT $JENKINS_HOSTNAME declarative-linter < Jenkinsfile

exit 0
