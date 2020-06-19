# Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=9000"

# replica means that replica (or server) was started and --debug will be at $3 position
replica_debug=$3
# client means that client was started and --debug will be at $5 position
client_debug=$5 

# add_replica means that add replica was started and --debug will be at $6 position
add_replica_debug=$6

#java -Dlogback.configurationFile="./config/logback.xml" -cp bin/*:lib/* $@

if [[ "$replica_debug" == "--debug" || "$client_debug" == "--debug" || "$add_replica_debug" == "--debug" ]]; then
    echo "Debug enabled"
    java "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=9000" -Dlogback.configurationFile="./config/logback-console-file.xml" -cp dist/*:lib/* $@
else
    java -Dlogback.configurationFile="./config/logback-console-file.xml" -cp dist/*:lib/* $@
fi


