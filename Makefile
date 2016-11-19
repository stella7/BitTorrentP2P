# Makefile for bittorrent project

JAVAC = /usr/lib/jvm/java-1.8.0/bin/javac
FLAGS = -nowarn -g
JAVA_FILES = $(wildcard *.java)
CLASSPATH = .:lib/*
# JAVA_FILES = $(wildcard tracker/*.java)

.PHONY = all clean

all: $(JAVA_FILES)
	@echo 'Making all...'
	@$(JAVAC) -cp $(CLASSPATH) $(FLAGS) $?

clean:
	rm -f $(JAVA_FILES:.java=.class)
