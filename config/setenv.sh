#!/bin/sh
export JAVA_OPTS="$JAVA_OPTS -Xms1G -Xmx16G"
export JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled"
export JAVA_OPTS="$JAVA_OPTS -Dehcache.config=$CATALINA_HOME/conf/ehcache.xml"
echo $JAVA_OPTS
