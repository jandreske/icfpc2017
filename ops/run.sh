#!/bin/sh

port=$1
ssh dreske@icfp.chainalysis.com "cd ~ && ./start.sh ${port}  && tail server.log"