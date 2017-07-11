from __future__ import division
import os
import glob
import argparse

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Count the number of victories of our agent')
    parser.add_argument('path', help='Path to directory to be analysed')
    parser.add_argument('-n', '--num-reps', help='Number of repetitions', type=int, required=False, default=30)
    parser.add_argument('-o', '--output', help='Save result to this file instead of showing on screen', required=False)
    parser.add_argument('-i', '--initial-epi', help='', required=False, default=0)
    parser.add_argument('-f', '--final-epi', help='', required=False)

    args = vars(parser.parse_args())

    num = 0
    count = 0
    num_games = 0
    rep_num = 0

    path = args['path']
    num_reps = args['num_reps']
    output = args['output']
    initial_epi = args['initial_epi']
    final_epi = args['final_epi']

    for rep_num in range(1, num_reps + 1):
        rep_name = os.path.join(path, '%s%s' % ('rep', str(rep_num).zfill(2)))
        if final_epi is None:
            final_epi = len(glob.glob(os.path.join(rep_name, '*.game'))) - 1
        for i, filename in enumerate(glob.glob(os.path.join(rep_name, '*.game'))):
            name = filename.split('episode_')[1]
            name = int(name.split('.')[0])
            if initial_epi <= name <= final_epi:
                num_games += 1
                f = open(filename, 'r')
                for line in f:
                    if line.startswith('jointRewards'):
                        line = f.next()
                        while True:
                            num = 0
                            line = line.split('[')
                            if not line[1].startswith('-'):
                                num += 1
                            line = f.next()
                            if line.startswith('states:'):
                                break
                count += num

    mean_games = num_games / rep_num
    mean_victories = count / rep_num

    if output is not None:
        output_file = open(output, 'w')
        output_file.write('Number of games: ' + str(mean_games) + '\n')
        output_file.write('Number of victories: ' + str(mean_victories) + '\n')
        output_file.write('Victory rate: ' + str("{:.0%}".format(mean_victories / mean_games)) + '\n')

    print 'Number of games: ' + str(mean_games)
    print 'Number of victories: ' + str(mean_victories)
    print 'Victory rate: ' + str("{:.0%}".format(mean_victories / mean_games))
