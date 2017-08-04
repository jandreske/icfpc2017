#!/bin/sh

(cd .. && ./gradlew clean build) || exit 1
scp ../build/libs/icfpc2017.jar dreske@icfp.chainalysis.com:~
scp start.sh dreske@icfp.chainalysis.com:~
