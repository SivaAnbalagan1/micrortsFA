#!/bin/bash
cwd=$(pwd)

cd ..

#classpath=.:../bin:../lib/jdom.jar:../lib/burlap-3.0.1-jar-with-dependencies.jar:lib/SCPSolver.jar:lib/LPSOLVESolverPack.jar
classpath=.:bin:lib/*

java -classpath $classpath -Djava.library.path=lib/ rl.validate.CompareEpisodes "$@" 

cd $cwd
