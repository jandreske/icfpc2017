#!/bin/sh

solver=$1
port=$2
ssh dreske@icfp.chainalysis.com "cd ~ && ./start.sh ${solver} ${port}  && tail server.log"