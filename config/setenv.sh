#!/bin/sh

# JAVA_OPTS
MEMORY="4G"
NORMAL="-server -d64 -Xms$MEMORY -Xmx$MEMORY"
HEAP_DUMP="-XX:+HeapDumpOnOutOfMemoryError"
HEADLESS="-Djava.awt.headless=true"
EHCACHE="-Dehcache.config=$CATALINA_HOME/conf/ehcache.xml"
EXTRAS="-XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled"

JAVA_PREFS_SYSTEM_ROOT="-Djava.util.prefs.systemRoot=$CATALINA_HOME/content/thredds/javaUtilPrefs -Djava.util.prefs.userRoot=$CATALINA_HOME/content/thredds/javaUtilPrefs"

JAVA_OPTS="$JAVA_OPTS $JAVA_PREFS_SYSTEM_ROOT $NORMAL $HEAP_DUMP $HEADLESS $PROFILE $EHCACHE $EXTRAS $JMX_OPTS"
echo "ncWMS Running with: $JAVA_OPTS"
