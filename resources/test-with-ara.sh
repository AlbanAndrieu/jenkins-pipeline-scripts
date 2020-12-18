#!/bin/bash -e
#set -xv

SCRIPT_NAME=$( basename $0 )
WORKING_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}"  )" && pwd  )"

# Forcing ansible cmd to use python3.6
#export PYTHON_MAJOR_VERSION=3.6

# shellcheck source=/dev/null
source "${WORKING_DIR}/run-python.sh"
RC=$?
if [ ${RC} -ne 0 ]; then
  echo ""
  echo -e "${red} ${head_skull} Sorry, ansible basics failed ${NC}"
  exit 1
fi

# shellcheck source=/dev/null
source "${WORKING_DIR}/step-2-helpers-color.sh"

set_default ANSIBLE_ROOT_DIR         "$( realpath ${WORKING_DIR} )/.."
set_default ARA_DIR                  "/tmp/.ara"
set_default PYTHON_EXECUTABLE        "python${PYTHON_MAJOR_VERSION}"
set_default OUTPUT_DIR               "ara"
set_default MOLECULE_TARGET          "test"
set_default MOLECULE_QUIET           "false"
set_default MOLECULE_DESTROY         "true"
set_default MOLECULE_OPTS            ""
set_default MOLECULE_ADDITIONAL_OPTS ""

usage()
{
    cat << USAGE >&2
This script can be used to run molecule tests on any Ansible role, that has molecule.yml configured.
In addition to running molecule tests, it is running ARA HTML Report (graphical report of all executed steps).
ARA environment is sourced each time before the tests execution and publishes report, even if the tests fail.

This makes browsing results and debugging more convenient, because you can open report in a browser,
instead of browsing through (potentially) large volume of console output.

Usage:
    $SCRIPT_NAME [-n|--never-destroy] [-t <target> | --target=<target>] [-q|---quiet] ansible_role_name(s)

ansible_role_name(s)
    The name of the directory in roles/ directory.
    Molecule configuration file (molecule.yml) must exist for
    the role under test.

-n, --never-destroy
    Sets molecule destroy flag to never. This flag should be used only in
    local development environment, to speed up test cycle. For example,
    when it is used, docker container used for testing will not be removed.

-t, --target
    Set molecule target action. Normally it should be "test" to run entire test
    suite. For local development, you may want to use just "converge", which
    will execute test playbook on the test environment (usually docker container),
    but will not perform any additional validation.

-q, --quiet
    Debug mode (molecule --debug) is used by default to provide most informative
    final report possible. For local development, it is reasonable to disable
    excess logging by using quiet (-q) flag.

Example use case:
    $SCRIPT_NAME git java
USAGE
    exit 1
}

assert_role_exists()
{
    ROLE_NAME=$1
    EXPECTED_ROLE_DIR="${ANSIBLE_ROOT_DIR}/roles/$ROLE_NAME"
    DEFAULT_MOLECULE_FILE="$EXPECTED_ROLE_DIR/molecule/default/molecule.yml"
    if [[ ! -d "$EXPECTED_ROLE_DIR" ]]
    then
        echo "Ansible role not found: $EXPECTED_ROLE_DIR"
        exit 1
    fi
    if [[ ! -f "$DEFAULT_MOLECULE_FILE" ]]
    then
        echo "Default molecule configuration file not found: $DEFAULT_MOLECULE_FILE"
        exit 1
    fi
}

set_python_executable()
{
    # if virtualenv is not used and there is system installation
    # of python3, it should be used
    if [[ -z $VIRTUAL_ENV ]]
    then
        if command -v python3.6
        then
            export PYTHON_EXECUTABLE="python3.6"
        else
            export PYTHON_EXECUTABLE="python"
        fi
    fi

    echo "PYTHON_EXECUTABLE : $PYTHON_EXECUTABLE"
}

set_molecule_opts()
{
    if ! $MOLECULE_QUIET
    then
        MOLECULE_OPTS+=" --debug"
    fi
    if ! $MOLECULE_DESTROY
    then
        if [[ $MOLECULE_TARGET == "test" ]]
        then
            MOLECULE_ADDITIONAL_OPTS+=" --destroy=never"
        fi
    fi
    export MOLECULE_OPTS
    export MOLECULE_ADDITIONAL_OPTS
}

test_ansible_role()
{
    cd "${ANSIBLE_ROOT_DIR}"
    rm -Rf ./ara-$ROLE_NAME* || true
    ROLE_NAME=$1
    # shellcheck disable=SC1090
    source <( $PYTHON_EXECUTABLE -m ara.setup.env )
    cd "$ANSIBLE_ROOT_DIR/roles/$ROLE_NAME"

    set +e && {
        set_molecule_opts
        cmd="molecule $MOLECULE_OPTS $MOLECULE_TARGET $MOLECULE_ADDITIONAL_OPTS"
        echo "Running molecule tests: $cmd"
        eval $cmd
        RS="$?"
        set -e
    }

    cd "$ANSIBLE_ROOT_DIR"
    # TODO ara reports are not more working
    echo "Running ara generate junit ${OUTPUT_DIR}-${ROLE_NAME}.xml"
    ara generate junit "${OUTPUT_DIR}-${ROLE_NAME}.xml" || true
    echo "Running ara generate html ${OUTPUT_DIR}-${ROLE_NAME}"
    ara generate html "${OUTPUT_DIR}-${ROLE_NAME}" || true

    # If there is any error in tests, it is thrown only after
    # HTML report is generate, so that even with errors we get output
    if [[ $RS -ne 0 ]]
    then
        echo "Molecule tests failed, quitting"
        #exit 2
    fi
}

ROLES_TO_TEST=()
while [[ $# -gt 0 ]]
do
    case "$1" in
        -h|--help)
            usage
        ;;
        -n|--never-destroy)
            MOLECULE_DESTROY=false
            shift 1
        ;;
        -t|--target)
            MOLECULE_TARGET="$2"
            shift 2
        ;;
        -q|--quiet)
            MOLECULE_QUIET=true
            shift 1
        ;;
        *)
            assert_role_exists $1
            ROLES_TO_TEST+=($1)
            shift 1
        ;;
    esac
done

set_python_executable
for ansible_role_name in "${ROLES_TO_TEST[@]}"
do
    echo "Testing Ansible role: $ansible_role_name"
    test_ansible_role $ansible_role_name
done
