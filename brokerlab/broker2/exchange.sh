#!/bin/bash
# client.sh
JAVA_HOME=/usr

# arguments to BrokerExchange
# $1 = hostname of where OnlineBroker is located
# $2 = port # where OnlineBroker is listening

${JAVA_HOME}/bin/java BrokerExchange $1 $2



