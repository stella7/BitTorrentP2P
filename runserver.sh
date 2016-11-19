#!/bin/bash

export CLASSPATH=".:lib/*"


java Tracker 5567 snorkel.uwaterloo.ca:2181 /$USER
