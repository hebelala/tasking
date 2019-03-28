#!/bin/bash

PRG="$0"
PRG_DIR=`dirname "$PRG"`
BASE_DIR=`cd "$PRG_DIR/.." >/dev/null; pwd`

nohup java -jar ${BASE_DIR}/lib/tasking-container-*.jar >> ${BASE_DIR}/bin/nohup.out 2>&1 &
