import matplotlib.pyplot as plt
import sys
import os
import glob
import argparse
from operator import itemgetter

if __name__ == '__main__':
    rewards_agent0 = []
    rewards_agent1 = []
    i = 0

    parser = argparse.ArgumentParser(description='')
    parser.add_argument('-p', '--path', help='Path to directory to be plotted', required=True)
    parser.add_argument('-e', '--episodes', help='Cumulative reward for X episodes', required=True)
    args = vars(parser.parse_args())

    window = int(args['episodes'])
    path = args['path']
    for filename in glob.glob(os.path.join(path, '*.game')):
        f = open(filename, 'r')

        while True:
            line = f.readline()
            if line.startswith("jointRewards"):
                while True:
                    reward = f.readline()
                    if reward.startswith("states"):
                        break
                    replacements = ('[', ']', ',')
                    for r in replacements:
                        reward = reward.replace(r, ' ')
                    reward = reward.split()
                    rewards_agent0.append((i, reward[1]))
                    rewards_agent1.append((i, reward[2]))
            elif not line:
                break
        i += 1

    points_agent0 = [0 for x in range(1000/window)]
    points_agent1 = [0 for x in range(1000/window)]

    num = map(itemgetter(0), rewards_agent0)
    reward = map(itemgetter(1), rewards_agent0)
    for i in range(len(rewards_agent0)):
        if (num[i] % window == 0 or num[i] == 0) and num[i] != num[i + 1]:
            initial = num[i]
            count = 0
            for j in range(initial, initial + 30):
                if count + i < len(num):
                    interval = num[i + count] - initial
                    count += 1
                    if interval < window and i + count < len(rewards_agent0):
                        points_agent0[num[i] / window] += float(reward[i + count])

    num = map(itemgetter(0), rewards_agent1)
    reward = map(itemgetter(1), rewards_agent1)
    for i in range(len(rewards_agent1)):
        if (num[i] % window == 0 or num[i] == 0) and num[i] != num[i + 1]:
            initial = num[i]
            count = 0
            for j in range(initial, initial + 30):
                if count + i < len(num):
                    interval = num[i + count] - initial
                    count += 1
                    if interval < window and i + count < len(rewards_agent1):
                        points_agent1[num[i] / window] += float(reward[i + count])

    line0, = plt.plot(points_agent0, color='b', label='Agent 0')
    line1, = plt.plot(points_agent1, color='r', label='Agent 1')
    plt.legend(handles=[line0, line1])
    plt.xlabel('Cumulative reward for each ' + str(window) + ' episodes')
    plt.show()
