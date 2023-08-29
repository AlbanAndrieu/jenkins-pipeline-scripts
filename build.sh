#!/bin/bash
set -e

WORKING_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# shellcheck source=/dev/null
source "${WORKING_DIR}/scripts/step-0-color.sh"

export REPO_TAG=${REPO_TAG:-"1.1.0"}

#./test-jenkins.sh

echo -e "${green} ./mvnw clean install -Dpipeline.stack.write=true ${NC}"

./mvnw clean install -Dpipeline.stack.write=true

#./gradlew wrapper --gradle-version=5.2.1 --distribution-type=bin
#./gradlew build

datree test ./k8s/*.yaml --only-k8s-files || true

git tag -l | xargs git tag -d # remove all local tags
git fetch -t                  # fetch remote tags

#git tag --delete v1.0.0
#git push --delete origin v1.0.0
echo -e "${green} git tag --delete v${REPO_TAG} ${NC}"
echo -e "${green} git tag v${REPO_TAG} ${NC}"
echo -e "${green} git push origin --tags ${NC}"

exit 0
