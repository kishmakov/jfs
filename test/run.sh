#!/bin/bash

for input in input/*
do
  base=`basename $input`
  java -jar -ea ../out/artifacts/jfs/jfs.jar $input


  if ! diff <(hexdump -C $base.jfs) <(hexdump -C refs/$base.jfs) >/dev/null 2>&1; then
    echo "$base fail"
  fi
  rm $base.jfs
done

