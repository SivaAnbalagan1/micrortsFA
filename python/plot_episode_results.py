import matplotlib.pyplot as plt
import numpy as np
import os
import argparse

parser = argparse.ArgumentParser(description='Plot action-values along episodes')
parser.add_argument('path', help='Path to file with action-value data')
parser.add_argument('outdir', help='Directory to generate plots in')

args = vars(parser.parse_args())

outdir = args['outdir']

dict = {}
opening = {}
early = {}
mid = {}
late = {}
end = {}

number = 0
name = ''

if __name__ == '__main__':
    game = ''
    agent = ''
    filename = args['path']
    file_name = os.path.split(filename)  # filename.split('\\')
    name = file_name[1]  # file_name[len(file_name) - 1]
    name = name.split('.')[0]
    number = name.split('_')[2]

    if number == 'final':
        number = 1000
    else:
        number = int(number)
    f = open(filename, 'r')
    for line in f:
        if line.startswith('<state id='):
            line = line.split('\'')
            stage = line[1]
            line = f.next()
            if stage == "OPENING":
                while True:
                    if line.startswith('</state>'):
                        break
                    line = line.strip()
                    line = line.split("\'")
                    opening.update({line[1]: float(line[3])})
                    line = f.next()
            elif stage == "EARLY":
                while True:
                    if line.startswith('</state>'):
                        break
                    line = line.strip()
                    line = line.split("\'")
                    early.update({line[1]: float(line[3])})
                    line = f.next()
            elif stage == "MID":
                while True:
                    if line.startswith('</state>'):
                        break
                    line = line.strip()
                    line = line.split("\'")
                    mid.update({line[1]: float(line[3])})
                    line = f.next()
            elif stage == "LATE":
                while True:
                    if line.startswith('</state>'):
                        break
                    line = line.strip()
                    line = line.split("\'")
                    late.update({line[1]: float(line[3])})
                    line = f.next()
            elif stage == "END":
                while True:
                    if line.startswith('</state>'):
                        break
                    line = line.strip()
                    line = line.split("\'")
                    end.update({line[1]: float(line[3])})
                    line = f.next()
    f.close()

multiple_bars = plt.figure()

N = 6
width = 0.1
x = range(6)
ind = np.arange(6)

y = opening.values()
z = early.values()
k = mid.values()
w = late.values()
d = end.values()

ax = plt.subplot()

ax.set_title('Value functions for agent ' + name + ' in game ' + str(number) + '\n')
ax.set_xticks(ind + width)
ax.set_xticklabels(('opening', 'early', 'mid', 'late', 'end'))

rects1 = ax.bar(ind, y, width=0.1, color='b', align='center')
rects2 = ax.bar(ind + width, z, width=0.1, color='#7CFC00', align='center')
rects3 = ax.bar(ind + width * 2, k, width=0.1, color='r', align='center')
rects4 = ax.bar(ind + width * 3, w, width=0.1, color='m', align='center')
rects5 = ax.bar(ind + width * 4, d, width=0.1, color='#FFA500', align='center')
rects6 = ax.bar(ind + width * 5, d, width=0.1, color='k', align='center')

ax.legend((rects1[0], rects2[0], rects3[0], rects4[0], rects5[0], rects6[0]),
          ('LightRush', 'HeavyRush', 'BuildBarracks', 'RangedRush', 'Expand', 'WorkerRush'))

plt.savefig(os.path.join(outdir, name + ".png"))