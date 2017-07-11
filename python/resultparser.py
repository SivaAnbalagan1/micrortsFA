import re
import sys
from collections import defaultdict

TIMEOUT = -1


def parse(filename):

    participants = set()
    timeouts = defaultdict(int)  # if key is not found, it creates a new one with 0 as its value
    scores = defaultdict(lambda: defaultdict(int))  # allows me to do scores[a][b] += 1 even when a and b were not keys previously
    num_games = defaultdict(lambda: defaultdict(int))

    '''
    Game register format is as follows:
    MATCH UP: AI1 vs AI2
    Winner: [number]  in [number] cycles
    AI1 : description
    AI2 : description

    we are interested in first two lines, which contain participants, winner and duration
    '''
    games = open("results.txt").readlines()

    current_game = None
    game_number = 0
    game_str = ''

    for line in games:
        # first line of a game: MATCH UP: AI1 vs AI2
        # the regular expression below matches that pattern
        match = re.match(r"^MATCH UP: (?P<ai1>.*) vs (?P<ai2>.*)$", line.strip())

        if match:
            ai1, ai2 = match.group('ai1'), match.group('ai2')
            participants.add(ai1)
            participants.add(ai2)
            current_game = [ai1, ai2]
            game_str = 'Game #%d: %s vs %s' % (game_number, ai1, ai2)

        # second line of a game: Winner: [number]  in [number] cycles
        # regular expression below matches that pattern
        match = re.match(r"^Winner:\s*(?P<winner>-?\d*)\s*in\s*(?P<duration>\d*)\s*cycles$", line.strip())
        if match:
            result = int(match.group('winner'))
            duration = int(match.group('duration'))

            # increment timeout count if needed
            if result == TIMEOUT:
                timeouts[ai1] += 1  # can do this even in first assignment since it is a defaultdict
                timeouts[ai2] += 1
                game_str += ' - TIMEOUT / duration: %d' % duration
            else:
                game_str += ' - won by %s(%d) - duration %d' % (current_game[result], result, duration)
                # calculates the score of first player (ai1): 1 if it won, -1 if it lost
                ai1_score = 1 if result == 0 else -1

                # updates scores for both players
                # assignment work even for first time, since it is a defaultdict
                scores[ai1][ai2] += ai1_score
                scores[ai2][ai1] -= ai1_score # player2 score is the negative of player1's

                # updates number of games between players
                num_games[ai1][ai2] += 1
                num_games[ai2][ai1] += 1

            #print(game_str)
            game_number += 1

    # prints everything out:
    print("Total number of games: %d" % game_number)
    participant_list = list(participants)

    print("Timeouts:")
    for ai in participant_list:
        print("%s: %d" % (ai, timeouts[ai]))

    print("\nOverall relative scores: ")
    for k, ai1 in enumerate(participant_list):
        for ai2 in participant_list[k:]:
            print("%s vs %s: %d (%d games)" % (ai1, ai2, scores[ai1][ai2], num_games[ai1][ai2]))


if __name__ == '__main__':
    parse(sys.argv[1])
