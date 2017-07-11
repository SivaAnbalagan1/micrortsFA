#!/bin/bash

# fixes the initial-q for all .xml files in a directory
if [ "$#" -ne 2 ]; then
	echo "Sets the initial-q for all .xml files in a directory. Usage:"
	echo "./fix-initial-q.sh directory initial-q"
	echo "directory: path to .xml files"
    echo "initial-q: the value for initial-q"
    exit
fi

cwd=$(pwd)

cd $1
find -name '*.xml' -exec sed -i -e 's/<initial-q .*\/>/<initial-q value="'$2'" \/>/g' {} \;

cd $cwd
