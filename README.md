# JFS

TOCTTOU
EAFP vs LBYL

## File System Description

### Principles

* utf-8 is used for names
* big-endian order is used
* all sizes are denominated in bytes
* data block size could vary in diapason 256B(?) — 16KB, must be multiple of 4
  bytes
* numerations for inodes and data blocks start from 1; number 0 is meant to be NIL
* inodes tree starts from the inode number 1
* unallocated inodes and data blocks are linked into lists; pointers to the heads
  of these lists are stored in the header section

### File Partition Overview

File is partitioned into 3 subsequently allocated sections:

1. Header
2. Inode Table
3. Data Blocks

#### Header Layout

Header has fixed size 32 bytes. It's layout is as follows:

Offset |Size     | Description
:-----:|:-------:|------------
 0     | 4       | magic number to sign JFS file, prescribed to be `0xAABBCCDD`
 4     | 2       | file system version, currently fixed to `0x0000`
 6     | 2       | size of data block, currently fixed to 4KB, `0x1000`
 8     | 4       | total number of inodes in file system
 12    | 4       | total number of blocks in file system
 16    | 4       | total number of unallocated inodes
 20    | 4       | total number of unallocated blocks
 24    | 4       | first unallocated inode id
 28    | 4       | first unallocated data block id


### Inode Layout

Inode consists of 64 bytes.

Offset |Size   | Description
:-----:|:-----:|------------
0      |1      | inode object type: `0x00` — directory, `0x01` — file
1      |3      | parent inode id (up to 16M inodes)
4      |4      | corresponding object size
8      |4      | id of data block #0
12     |4      | id of data block #1
16     |4      | id of data block #2
20     |4      | id of data block #3
24     |4      | id of data block #4
28     |4      | id of data block #5
32     |4      | id of data block #6
36     |4      | id of data block #7
40     |4      | id of data block #8
44     |4      | id of data block #9
48     |4      | id of data block #10
52     |4      | id of data block #11
56     |4      | Singly indirect pointer
60     |4      | Doubly indirect pointer

### Directory Organisation

Directories are supposed to be a specific files, holding linked lists of pairs
`{inode number, myName}`. Implementation is motivated by [ext2 directory organisation](http://www.nongnu.org/ext2-doc/ext2.html#DIRECTORY).

In contrast with ext2 organization, we don't store per entry size. This is
motivated by the decision to rewrite each block fully after modification.

Each directory block starts with 2 byte describing unused space at the end of
this block followed by directory entries.

#### Directory Block Layout

If we denote block size with `BS`, then

Offset          |Size          | Description
:--------------:|:------------:|------------
0               |2             | block unused size, `BU`
2               |S<sub>1</sub> | first entry
2+S<sub>1</sub> |S<sub>2</sub> | second entry
...             |...           | ...
`BS - BU`       |`BU`          | unused space


#### Directory Entry Layout

Offset |Size   | Description
:-----:|:-----:|------------
0      |4      | inode id
4      |1      | myName length, `L`
5      |`L`    | myName characters


