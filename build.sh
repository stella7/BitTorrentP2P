#!/bin/sh

#
# Wojciech Golab, 2016
#

JAVA_CC=/usr/lib/jvm/java-1.8.0/bin/javac


export CLASSPATH=".:lib/*"

echo --- Cleaning
rm -f *.class


echo --- Compiling Java
$JAVA_CC -version
$JAVA_CC *.java

