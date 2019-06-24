#!/usr/bin/env bash

set -x

echo "Starting job with JAR"

ls -la

cd /app/db

ls -la

cd ..

arguments_list() {
    echo "Example:
          $0
            --environment           <local | dev | prod>
            --schemas               <public>
            --action                <migrate | repair>
            "
}

if [ $# -lt 2 ]
then
    echo "Arguments not found..."
    arguments_list
    exit -1
else

    while [ $# -gt 0 ]
    do
    key="$1"

    case $key in
        --environment)
            ENVIRONMENT="$2"
            shift # past argument
        ;;
        --schemas)
            SCHEMAS_TO_RUN="$2"
            shift # past argument
        ;;
        --action)
            ACTION_TYPE="$2"
            shift # past argument
        ;;
        *)
          echo "Unknown argument: Key: $key - Value: $2"
          shift
        ;;
    esac
    shift # past argument or value
    done

    if [ -z "$ACTION_TYPE" ]
    then
        ACTION_TYPE="migrate"
    fi

    if [ -z "$SCHEMAS_TO_RUN" ]
    then
        java -jar ${BUILD_JAR_NAME} \
        --environment ${ENVIRONMENT} \
        --action ${ACTION_TYPE}
    else
        java -jar ${BUILD_JAR_NAME} \
        --environment ${ENVIRONMENT} \
        --schemas ${SCHEMAS_TO_RUN} \
        --action ${ACTION_TYPE}
    fi

    APP_RUN_STATUS="${?}"

    echo "Job finished with JAR ${BUILD_JAR_NAME} built from BRANCH $GIT_BRANCH and COMMIT_ID $GIT_COMMIT_ID."

    exit ${APP_RUN_STATUS}
fi
