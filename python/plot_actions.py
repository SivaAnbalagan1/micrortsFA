import os
import argparse
import glob
import matplotlib.pyplot as plt

parser = argparse.ArgumentParser(description='Plot action-values along episodes')
parser.add_argument('path', help='Path to file with action-value data')
parser.add_argument('outdir', help='Directory to generate plots in')
parser.add_argument('episodes', help='Number of episodes tested')

args = vars(parser.parse_args())

outdir = args['outdir']

number = 0
results = []

stage = ''
for i, filename in enumerate(glob.glob(os.path.join(args['path'], '*.txt'))):
    if filename.__contains__("q_learner"):
        file_name = os.path.split(filename) # filename.split('/')
        name = file_name[1] #[len(file_name) - 1]
        number = name.split('_')[2].split('.')[0]

        if number == 'final':
            number = args['episodes']
        else:
            number = int(number)
        f = open(filename, 'r')
        for line in f:
            if line.startswith('<state id='):
                line = line.split('\'')
                stage = line[1]
                line = f.next()
                while True:
                    if line.startswith('</state>'):
                        break
                    line = line.strip()
                    line = line.split("\'")
                    results.append((number, stage, line[1], line[3]))
                    line = f.next()
        f.close()

results.sort(key=lambda x: x[0])

opening = [item for item in results if item[1] == 'OPENING']

light_opening = [(elem0, elem3) for elem0, elem1, elem2, elem3 in opening if elem2 == 'LightRush']
heavy_opening = [(elem0, elem3) for elem0, elem1, elem2, elem3 in opening if elem2 == 'HeavyRush']
barracks_opening = [(elem0, elem3) for elem0, elem1, elem2, elem3 in opening if elem2 == 'BuildBarracks']
ranged_opening = [(elem0, elem3) for elem0, elem1, elem2, elem3 in opening if elem2 == 'RangedRush']
expand_opening = [(elem0, elem3) for elem0, elem1, elem2, elem3 in opening if elem2 == 'Expand']
worker_opening = [(elem0, elem3) for elem0, elem1, elem2, elem3 in opening if elem2 == 'WorkerRush']

plt.figure(1)
plt.plot(*zip(*light_opening), label='LightRush')
plt.plot(*zip(*heavy_opening), label='HeavyRush')
plt.plot(*zip(*barracks_opening), label='BuildBarracks')
plt.plot(*zip(*ranged_opening), label='RangedRush')
plt.plot(*zip(*expand_opening), label='Expand')
plt.plot(*zip(*worker_opening), label='WorkerRush')
plt.legend(loc=3, borderaxespad=0.)
plt.savefig(os.path.join(outdir, "01_opening.png"))

early = [item for item in results if item[1] == 'EARLY']

light_early = [(elem0, elem3) for elem0, elem1, elem2, elem3 in early if elem2 == 'LightRush']
heavy_early = [(elem0, elem3) for elem0, elem1, elem2, elem3 in early if elem2 == 'HeavyRush']
barracks_early = [(elem0, elem3) for elem0, elem1, elem2, elem3 in early if elem2 == 'BuildBarracks']
ranged_early = [(elem0, elem3) for elem0, elem1, elem2, elem3 in early if elem2 == 'RangedRush']
expand_early = [(elem0, elem3) for elem0, elem1, elem2, elem3 in early if elem2 == 'Expand']
worker_early = [(elem0, elem3) for elem0, elem1, elem2, elem3 in early if elem2 == 'WorkerRush']

plt.figure(2)
plt.plot(*zip(*light_early), label='LightRush')
plt.plot(*zip(*heavy_early), label='HeavyRush')
plt.plot(*zip(*barracks_early), label='BuildBarracks')
plt.plot(*zip(*ranged_early), label='RangedRush')
plt.plot(*zip(*expand_early), label='Expand')
plt.plot(*zip(*worker_early), label='WorkerRush')
plt.legend(loc=3, borderaxespad=0.)
plt.savefig(os.path.join(outdir, "02_early.png"))

mid = [item for item in results if item[1] == 'MID']

light_mid = [(elem0, elem3) for elem0, elem1, elem2, elem3 in mid if elem2 == 'LightRush']
heavy_mid = [(elem0, elem3) for elem0, elem1, elem2, elem3 in mid if elem2 == 'HeavyRush']
barracks_mid = [(elem0, elem3) for elem0, elem1, elem2, elem3 in mid if elem2 == 'BuildBarracks']
ranged_mid = [(elem0, elem3) for elem0, elem1, elem2, elem3 in mid if elem2 == 'RangedRush']
expand_mid = [(elem0, elem3) for elem0, elem1, elem2, elem3 in mid if elem2 == 'Expand']
worker_mid = [(elem0, elem3) for elem0, elem1, elem2, elem3 in mid if elem2 == 'WorkerRush']

plt.figure(3)
plt.plot(*zip(*light_mid), label='LightRush')
plt.plot(*zip(*heavy_mid), label='HeavyRush')
plt.plot(*zip(*barracks_mid), label='BuildBarracks')
plt.plot(*zip(*ranged_mid), label='RangedRush')
plt.plot(*zip(*expand_mid), label='Expand')
plt.plot(*zip(*worker_mid), label='WorkerRush')
plt.legend(loc=3, borderaxespad=0.)
plt.savefig(os.path.join(outdir, "03_mid.png"))

late = [item for item in results if item[1] == 'LATE']

light_late = [(elem0, elem3) for elem0, elem1, elem2, elem3 in late if elem2 == 'LightRush']
heavy_late = [(elem0, elem3) for elem0, elem1, elem2, elem3 in late if elem2 == 'HeavyRush']
barracks_late = [(elem0, elem3) for elem0, elem1, elem2, elem3 in late if elem2 == 'BuildBarracks']
ranged_late = [(elem0, elem3) for elem0, elem1, elem2, elem3 in late if elem2 == 'RangedRush']
expand_late = [(elem0, elem3) for elem0, elem1, elem2, elem3 in late if elem2 == 'Expand']
worker_late = [(elem0, elem3) for elem0, elem1, elem2, elem3 in late if elem2 == 'WorkerRush']

plt.figure(4)
plt.plot(*zip(*light_late), label='LightRush')
plt.plot(*zip(*heavy_late), label='HeavyRush')
plt.plot(*zip(*barracks_late), label='BuildBarracks')
plt.plot(*zip(*ranged_late), label='RangedRush')
plt.plot(*zip(*expand_late), label='Expand')
plt.plot(*zip(*worker_late), label='WorkerRush')
plt.legend(loc=3, borderaxespad=0.)
plt.savefig(os.path.join(outdir, "04_late.png"))

end = [item for item in results if item[1] == 'END']

light_end = [(elem0, elem3) for elem0, elem1, elem2, elem3 in end if elem2 == 'LightRush']
heavy_end = [(elem0, elem3) for elem0, elem1, elem2, elem3 in end if elem2 == 'HeavyRush']
barracks_end = [(elem0, elem3) for elem0, elem1, elem2, elem3 in end if elem2 == 'BuildBarracks']
ranged_end = [(elem0, elem3) for elem0, elem1, elem2, elem3 in end if elem2 == 'RangedRush']
expand_end = [(elem0, elem3) for elem0, elem1, elem2, elem3 in end if elem2 == 'Expand']
worker_end = [(elem0, elem3) for elem0, elem1, elem2, elem3 in end if elem2 == 'WorkerRush']

plt.figure(5)
plt.plot(*zip(*light_end), label='LightRush')
plt.plot(*zip(*heavy_end), label='HeavyRush')
plt.plot(*zip(*barracks_end), label='BuildBarracks')
plt.plot(*zip(*ranged_end), label='RangedRush')
plt.plot(*zip(*expand_end), label='Expand')
plt.plot(*zip(*worker_end), label='WorkerRush')
plt.legend(loc=3, borderaxespad=0.)
plt.savefig(os.path.join(outdir, "05_end.png"))
