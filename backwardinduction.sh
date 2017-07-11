#!/bin/bash

#classpath=.:../bin:../lib/jdom.jar:../lib/burlap-3.0.1-jar-with-dependencies.jar:lib/SCPSolver.jar:lib/LPSOLVESolverPack.jar
classpath=.:bin:lib/*

echo "Launching backward induction..."

java -classpath $classpath -Xmx2G -Xss4G -Djava.library.path=lib/ rl.planners.BackwardInduction "$@" 

echo "Done."
