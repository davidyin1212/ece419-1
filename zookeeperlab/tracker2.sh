#!/bin/bash
#JAVA_HOME=/cad2/ece419s/java/jdk1.6.0/
JAVA_HOME=/usr

#if [[ "$#" -ne 2 ]]; then
#	echo "Usage: client.sh [zoohost] [zooport]";
#else
	${JAVA_HOME}/bin/java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. Tracker localhost 8498 3457;
#fi

