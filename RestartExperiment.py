#!/usr/bin/python3

import os
import sys
import time
import argparse
import subprocess


def manage_experiment(num_execs, num_concurrent, output_prefix):
    
    processes = set()
    total_runs = 0
    total_finished = 0
    
    while total_finished < num_execs:
        # checks whether some process has finished
        finished_procs = []
        for proc in processes:
            if proc.poll() is not None: # it has terminated!
                total_finished += 1
                finished_procs.append(proc)
            
        # removes finished procecess from the set
        for finished in finished_procs:
            print('A process has finished')
            processes.remove(finished)
            
        # if we're already fully loaded, sleep
        if len(processes) >= num_concurrent:
            time.sleep(1)
            
        # otherwise, start a process (respecting the limit)
        elif total_runs < num_execs:
            total_runs += 1
            print('Starting process number %d' % total_runs)
           
            # finally opens the new process
            new_proc = subprocess.Popen(
                './rlexperimentRestartable.sh -restart %s/rep%s' \
                % (output_prefix, str(total_runs).zfill(2)),
                shell=True
            )
            
            processes.add(new_proc)
            
            
    # all processes have finished
    print('All processes finished')
    
if __name__ == '__main__':
    # process command line args 
    parser = argparse.ArgumentParser(description='Manages concurrent runs of an experiment')
    
    parser.add_argument(
        '-n', '--number-reps', type=int, required=True, 
        help='The total number of repetitions to execute'
    )
    
    parser.add_argument(
        '-s', '--simultaneous', type=int, required=True, 
        help='The number of repetitions that can run simultaneously'
    )
       
    parser.add_argument(
        '-restart', '--output-prefix', type=str, required=True, 
        help='Root directory where experiment data will be written and ser file stored'
    )
    
    args = parser.parse_args()
    
    manage_experiment(
        args.number_reps, args.simultaneous, 
        args.output_prefix
    )


    
            
            
        
            
