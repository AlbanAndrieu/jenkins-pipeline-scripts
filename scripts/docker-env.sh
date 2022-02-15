#!/bin/bash
#set -xv

WORKING_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# shellcheck source=/dev/null
source "${WORKING_DIR}/step-0-color.sh"

# shellcheck disable=SC2154
echo -e "${magenta} Building TEST runtime image ${NC}"
# shellcheck disable=SC2154
echo -e "${green} pip install docker-compose==1.25.0 ${NC}"

if [ -n "${DOCKER_BUILD_ARGS}" ]; then
  # shellcheck disable=SC2154
  echo -e "${green} DOCKER_BUILD_ARGS is defined ${happy_smiley} : ${DOCKER_BUILD_ARGS} ${NC}"
else
  # shellcheck disable=SC2154
  echo -e "${red} ${double_arrow} Undefined build parameter ${head_skull} : DOCKER_BUILD_ARGS, use the default one ${NC}"
  export DOCKER_BUILD_ARGS="--pull --no-cache "
  #export DOCKER_BUILD_ARGS="--pull --no-cache --build-arg ANSIBLE_VAULT_PASS=${ANSIBLE_VAULT_PASS} --squash"
  echo -e "${magenta} DOCKER_BUILD_ARGS : ${DOCKER_BUILD_ARGS} ${NC}"
fi

if [ -n "${CST_CONFIG}" ]; then
  # shellcheck disable=SC2154
  echo -e "${green} CST_CONFIG is defined ${happy_smiley} : ${CST_CONFIG} ${NC}"
else
  # shellcheck disable=SC2154
  echo -e "${red} ${double_arrow} Undefined build parameter ${head_skull} : CST_CONFIG, use the default one ${NC}"
  #export CST_CONFIG="docker/ubuntu16/config.yaml" v
  export CST_CONFIG="docker/centos7/config.yaml" # runtime image
  echo -e "${magenta} CST_CONFIG : ${CST_CONFIG} ${NC}"
fi

if [ -n "${DOCKER_NAME}" ]; then
  # shellcheck disable=SC2154
  echo -e "${green} DOCKER_NAME is defined ${happy_smiley} : ${DOCKER_NAME} ${NC}"
else
  # shellcheck disable=SC2154
  echo -e "${red} ${double_arrow} Undefined build parameter ${head_skull} : DOCKER_NAME, use the default one ${NC}"
  export DOCKER_NAME=${DOCKER_NAME:-"jenkins-pipeline-script"}
  echo -e "${magenta} DOCKER_NAME : ${DOCKER_NAME} with ${CST_CONFIG} ${NC}"
fi

if [ -n "${DOCKER_TAG}" ]; then
  # shellcheck disable=SC2154
  echo -e "${green} DOCKER_TAG is defined ${happy_smiley} : ${DOCKER_TAG} ${NC}"
else
  # shellcheck disable=SC2154
  echo -e "${red} ${double_arrow} Undefined build parameter ${head_skull} : DOCKER_TAG, use the default one ${NC}"
  export DOCKER_TAG=${DOCKER_TAG:-"latest"}
  echo -e "${magenta} DOCKER_TAG : ${DOCKER_TAG} ${NC}"
fi

if [ -n "${DOCKER_FILE}" ]; then
  # shellcheck disable=SC2154
  echo -e "${green} DOCKER_FILE is defined ${happy_smiley} : ${DOCKER_FILE} ${NC}"
else
  # shellcheck disable=SC2154
  echo -e "${red} ${double_arrow} Undefined build parameter ${head_skull} : DOCKER_FILE, use the default one ${NC}"
  export DOCKER_FILE=${DOCKER_FILE:-"Dockerfile"}
  echo -e "${magenta} DOCKER_FILE : ${DOCKER_FILE} ${NC}"
fi

readonly DOCKER_REGISTRY=${DOCKER_REGISTRY:-""}
readonly DOCKER_REGISTRY_HUB=${DOCKER_REGISTRY_HUB:-"index.docker.io/v1"}
readonly DOCKER_ORGANISATION=${DOCKER_ORGANISATION:-"nabla"}
readonly DOCKER_ORGANISATION_HUB=${DOCKER_ORGANISATION_HUB:-"nabla"}
readonly DOCKER_REGISTRY_HUB_CREDENTIAL=${DOCKER_REGISTRY_HUB_CREDENTIAL:-"hub-docker-nabla"}

readonly DOCKER_USERNAME=${DOCKER_USERNAME:-""}
export DOCKER_NAME=${DOCKER_NAME:-"jenkins-pipeline-script"}
export DOCKER_TAG=${DOCKER_TAG:-"latest"}
