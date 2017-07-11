#!/bin/bash


if [ "$#" -lt 3 ]; then
	echo "Usage: ./extract-knowledge.sh file destdir num-reps"
	exit
fi

cwd=$(pwd)

# creates temp. dir to extract files
mkdir /tmp/extracted
cd /tmp/extracted

# extraction
echo "Extracting from $1"
tar xvf $1 --no-anchored q_learner_final.txt

# goes to destination directory
cd $cwd
mkdir $2
cd $2

echo "Moving files to $2"
for j in $(seq -f "%02g" 1 $3); do
	# using * because I assume there's only one directory inside extracted
	mv /tmp/extracted/*/rep"$j"/q_learner_final.txt "q_final_rep$j.xml"
done

# cleanup
echo "Cleaning up"
rm -rf /tmp/extracted

cd $cwd

echo "Done. Resulting files are in $2"
