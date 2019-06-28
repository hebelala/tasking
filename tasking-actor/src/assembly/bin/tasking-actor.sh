#!/bin/bash
#
# Copyright © 2019 hebelala (hebelala@qq.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


PRG="$0"
PRG_DIR=`dirname "$PRG"`
BASE_DIR=`cd "$PRG_DIR/.." >/dev/null; pwd`

nohup java -jar ${BASE_DIR}/lib/tasking-actor-*.jar >> ${BASE_DIR}/bin/nohup.out 2>&1 &
