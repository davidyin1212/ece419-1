#!/bin/bash
# client.sh
#ECE419_HOME=/cad2/ece419s/
JAVA_HOME=/usr

# arguments to BrokerClient
# $1 = hostname of where OnlineBroker is located
# $2 = port # where OnlineBroker is listening

${JAVA_HOME}/bin/java BrokerClient $1 $2



