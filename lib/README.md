Please add all .lib files in this directory into your build path.
Otherwise you may face some problems.

Besides, you must use SCPSolver and LBSOLVESolverPack from this directory 
(instead of BURLAP's defaults) in order to use PersistentMultiAgentQLearning 
without running into SIGSEGV on the JVM.

Please follow these steps:

- Add SCPSolver.jar and LPSOLVESolverPack.jar to the build path;
- Right click on project main directory -> Build Path -> Configure Build Path;
- Select the Libraries tab;
- Expand the JRE System library option and select Native library location;
- Click on Edit, point to this lib directory and click OK.

These steps were adapted from:
https://examples.javacodegeeks.com/java-basics/java-library-path-what-is-it-and-how-to-use/
 