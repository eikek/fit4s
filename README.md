# fit4s - encode and decode ANT FIT files with Scala

This is a library for Scala 2.13 and 3.

## Format Protocol

The protocol and file formats a specified in the following documents:

- [Flexible & Interoperable Data Transfer (FIT) Protocol](doc/D00001275 Flexible & Interoperable Data Transfer (FIT) Protocol Rev 2.3.pdf)
- [FIT File Types Description](doc/D00001309 FIT File Types Description Rev 2.2.pdf)

The first contains the overview of the protocol and the latter defines
known FIT file types.

These files are contained in the FIT SDK zip archive which can be found 
around [here](https://developer.garmin.com/fit/protocol/)

### Overview

A FIT file consists of

1. file header (min. 12 bytes, preferred 14 bytes, maybe larger)
2. data records
3. CRC (2 bytes)

Data records contain the main content. Records have a 1-byte header
and a content and are split in two classes:

1. definition messages: define upcoming data messages
2. data messages: data fields in format described by preceding
   definition message

Data messages may be one of

- normal data message
- compressed timestamp data message

which is indicated by the header.
