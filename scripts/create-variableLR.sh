#!/bin/bash

echo "Creating variable L.R. experiments at $1"

mkdir $1/variableLR
echo "Directory created"

cwd=$(pwd)

cp $1/*.xml $1/variableLR
echo "Files copied"

cd $1/variableLR
find -name '*.xml' -exec sed -i -e 's/<learning-rate .* \/>/<learning-rate type="exponential-decay" initial="1.0" final="0.1" rate="0.995405417" \/> <!-- From 1.0 to 0.1 in 500 episodes -->/g' {} \;
echo "Learning rate changed"

cd $cwd
 

echo "Done."
