# JFS

TOCTTOU
EAFP vs LBYL

## File System Description

### Principles

* All sizes are denominated in bytes
* Data block size could vary in diapason 256B(?) — 16KB, must be multiple of 4
  bytes
* Inodes and data blocks numerations start from 1; number 0 is meant to be NIL
* Inodes tree starts from the inode number 1
* Vacant inodes and data blocks are linked into lists; pointers to the heads
  of these lists are stored in the header section

### File Partition Overview

Section        | Size    | Description
--------------:|:-------:|------------
Header         | 18      | Current file geometry description
-              | 4       | Signature to testify JFS file, must be `0xAABBCCDD`
-              | 2       | `DBS`, size of data block in bytes, like `0x1000`
-              | 4       | `IN`, number of inodes
-              | 4       | Pointer to the first vacant inode
-              | 4       | Pointer to the first vacant data block
Inodes section |64 x `IN`| Starts after header, contains `IN` cells describing corresponding inodes
Data section   |       ? | Starts after inodes section and stretches till the end of the file

### Inode Overview

Inode consists of 64 bytes.

Size   | Description
:-----:|------------
1      | Inode object type: `0x00` — directory, `0x01` — file
3      | Pointer to parent inode (up to 16M inodes)
4      | Inode object size
4      | Pointer to data block #0
4      | Pointer to data block #1
4      | Pointer to data block #2
4      | Pointer to data block #3
4      | Pointer to data block #4
4      | Pointer to data block #5
4      | Pointer to data block #6
4      | Pointer to data block #7
4      | Pointer to data block #8
4      | Pointer to data block #9
4      | Pointer to data block #10
4      | Pointer to data block #11
4      | Singly indirect pointer
4      | Doubly indirect pointer
  