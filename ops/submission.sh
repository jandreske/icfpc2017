#!/bin/bash
./gradlew build
cp build/libs/icfpc2017.jar dist
cp -R src dist
cd dist
tar cf - * | gzip > ../submission.tar.gz
