from __future__ import division
import os
import glob
import argparse
import locale
import re

"""
This script counts the number of victories of the first player 
in .xml files by checking the resulting PhysicalGameState
"""

def run(directories, num_reps, output, initial_epi, final_epi, verbose):

    num = 0
    count = 0
    num_games = 0
    rep_num = 0

    for repetition in directories:
        if final_epi is None:
            final_epi = len(glob.glob(os.path.join(repetition, '*.xml'))) - 1

        for i, filename in enumerate(sorted(glob.glob(os.path.join(repetition, '*.xml')))): #sorted
        #for i, filename in enumerate(glob.glob(os.path.join(repetition, '*.xml'))): #unsorted
            if verbose:
                print('file: %s' % filename)

            # matches file name with pattern 'episode_num.xml'
            basename = os.path.basename(filename)
            result = re.search('^(\w+)_(\d+)\.(\w+)', basename)

            if result is None: #file name does not match
                continue

            name, episode_str, ext = result.groups()

            if initial_epi <= int(episode_str) <= final_epi:
                num_games += 1
                lines = open(filename, 'r').readlines()

                playerUnits = [0, 0]
                
                for line in lines:
                    # pattern is: unit_type(id)(owner, (x,y), hp, resources)
                    # i'm interested in owner, so i just wrap owner with parentheses
                    line_pattern = re.search('\s*\w+\(\d+\)\((\d+).*', line)
                    if line_pattern is not None:
                        # print(line)
                        owner, = line_pattern.groups()  # comma discards the remainder of the tuple
                        if int(owner) != -1:
                            playerUnits[int(owner)] += 1
                # finished processing the file, now increments the win count
                # first condition checks for draw and second tests if player 0 is the winner
                # print('%s: %d vs %d' % (basename, playerUnits[0], playerUnits[1]))
                if not all(playerUnits) and playerUnits[0] > 0:
                    #print("victory in %s" % basename)
                    count += 1

    mean_games = num_games / num_reps
    mean_victories = count / num_reps

    if output is not None:
        output_file = open(output, 'w')
        output_file.write('Number of games: %d\n' % mean_games)
        output_file.write('Mean #victories: %f\n' % mean_victories)
        output_file.write('Victory rate: {:.0%}\n'.format(mean_victories / mean_games))

    if verbose:
        print('Dirs: %s' % directories)
        print('Number of games: %d' % mean_games)
        print('Mean #victories: %f' % mean_victories)
        print('%mean victories: {:.3%}'.format(mean_victories / mean_games))

    else:
        print('{0:n}'.format(mean_victories / mean_games))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Count the number of victories of our agent')
    parser.add_argument(
        'dir',
        help='List of directories to analyse, each one is treated as an experiment repetition',
        nargs='+'
    )
    parser.add_argument(
        '-a', '--aggregate', required=False, action='store_true',
        help='Calculate the mean results from a list of directories, otherwise shows them individually'
    )
    parser.add_argument(
        '-o', '--output', required=False,
        help='Save result to this file instead of showing on screen'
    )
    parser.add_argument(
        '-i', '--initial-epi', type=int, required=False, default=0,
        help='First episode to consider'
    )
    parser.add_argument(
        '-f', '--final-epi', type=int, help='Last episode to consider', required=False
    )
    parser.add_argument(
        '-v', '--verbose', help='Output additional info?', action='store_true'
    )
    parser.add_argument(
        '-l', '--locale', choices=['pt_BR.utf8', 'en_US.utf-8'], default='pt_BR.utf-8',
        help='"pt_BR.utf8" for comma as decimal separator, "en_US.utf-8" for dot. '
             'If pt_BR is unsupported, install it with sudo apt-get install language-pack-pt'
    )

    args = vars(parser.parse_args())

    locale.setlocale(locale.LC_NUMERIC, args['locale'])

    # if aggregate is activated, runs once with the list of directories to output the average
    if args['aggregate']:
        run(
            args['dir'], len(args['dir']), args['output'],
            args['initial_epi'], args['final_epi'], args['verbose']
        )
    # if aggregate is deactivated, runs once for each dir, outputting to stdout
    else:
        for directory in args['dir']:
            # first argument must be a list, so we pass a single-member one
            run(
                [directory], 1, None, args['initial_epi'],
                args['final_epi'], args['verbose']
            )
