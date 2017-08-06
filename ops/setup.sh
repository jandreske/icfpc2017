#!/bin/sh

folder=$1
solver=$2
port=$3

ssh dreske@icfp.chainalysis.com "cd ~ && mkdir ${folder} && cp icfpc2017.jar ${folder} && cp start.sh ${folder}"
ssh dreske@icfp.chainalysis.com "cd ~/${folder} && ./start.sh ${solver} ${port} " &
ssh dreske@icfp.chainalysis.com "cd ~/${folder} && tail -F server.log"

