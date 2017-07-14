#!/usr/bin/python3

import os
import sys
import time
import argparse
import subprocess


def manage_experiment(num_execs, num_concurrent, experiment_config, output_prefix, 
policy1_prefix, policy2_prefix, other_options):
    
    os.makedirs(output_prefix, exist_ok=True)
    
    processes = set()
    total_runs = 0
    total_finished = 0
    
    # prevents errors when concatenating...
    if other_options is None:
        other_options = ''
    
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
            
            # checks if we need to specify the player's policy
            additional_args = other_options
            
            if policy1_prefix is not None:
                additional_args += '-player1policy %s%s.xml ' % (policy1_prefix, str(total_runs).zfill(2))
                
            if policy2_prefix is not None:
                additional_args += '-player2policy %s%s.xml ' % (policy2_prefix, str(total_runs).zfill(2))
            # error: other_options is growing up!
            #print(other_options, '-player1policy %s%s.xml ' % (policy1_prefix, str(total_runs).zfill(2)))
            
            # finally opens the new process
            new_proc = subprocess.Popen(
                './rlexperimentRestartable.sh -c %s -o %s/rep%s %s' \
                % (experiment_config, output_prefix, str(total_runs).zfill(2), additional_args),
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
        '-c', '--experiment-config', type=str, required=True, 
        help='Path to experiment configuration'
    )

    parser.add_argument(
        '-o', '--output-prefix', type=str, required=True, 
        help='Root directory where experiment data will be written'
    )
    
    parser.add_argument(
        '--policy1-prefix', type=str, required=False,
        help="Prefix of policy file of player 1"
    )
    
    parser.add_argument(
        '--policy2-prefix', type=str, required=False,
        help="Prefix of policy file of player 2"
    )
    
    parser.add_argument(
        '-r', '--remaining-options', type=str, required=False, 
        help='Remaining experiment options (use quotes - e.g. "--option1 value1 --option2 value2")'
    )
    
    args = parser.parse_args()
    
    manage_experiment(
        args.number_reps, args.simultaneous, 
        args.experiment_config, args.output_prefix,
        args.policy1_prefix, args.policy2_prefix,
        args.remaining_options
    )


    
            
            
        
            
