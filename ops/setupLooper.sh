#!/bin/sh

folder=$1
solver=$2
port=$3
(cd .. && ./gradlew clean build) || exit 1
ssh dreske@icfp.chainalysis.com "cd ~ && mkdir ${folder}"
scp ../build/libs/icfpc2017.jar dreske@icfp.chainalysis.com:~/${folder}
scp start.sh dreske@icfp.chainalysis.com:~/${folder}

while true; do
    ssh dreske@icfp.chainalysis.com "cd ~/${folder} && ./start.sh ${solver} ${port}  && tail server.log | grep 'RANKING\|SCORE'"
    sleep 10s
done
