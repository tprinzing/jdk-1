#!/usr/bin/bash
#
# run the test to measure the overhead of the various jfr states in
# the boilerplate code that surrounds the call to the actual method being
# measured.
export BUILD=../../build
export JAVA_HOME=$BUILD/linux-x86_64-server-release/jdk
export PATH=$JAVA_HOME/bin:$PATH
mvn clean install

#$JAVA_HOME/bin/java -jar target/benchmarks.jar -wi 10 -w 10s -i 20 -r 60s
$JAVA_HOME/bin/java -jar target/benchmarks.jar 
