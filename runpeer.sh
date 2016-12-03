#!/bin/bash

export CLASSPATH=".:lib/*"


java -Xmx128m Peer $1 $2 $3
