# fit4s - encode and decode ANT FIT files with Scala

This is a library for Scala 2.13 and 3.

## TODO

- [x] fix missing session values using corresponding records
- [x] add tags to import cmd
- [x] parallel directory import (max(3, cpus - 2))
- better error when init was not called, or just do initialize
- add LapMsg to database
- [x] try fix timestamps for GarminSwim
  - not possible to get the correct time, bc it is based on "seconds
    from device power on" (when batteries got inserted). Since
    batteries have been changed I can't know the correct value)
  - The activity contains a local timestamp, this made sense for a
    file, so I'm using this as base to correct all values that are
    below the minimum
- cleanup playground test mess
- [x] summary on db
- nicer prints, remove lines with no value, etc
- have json and pretty output
- list activities, option to list files only 
- add/set/remove/rename tags
- update command
- [x] check path before adding location record
- config file with db data and timezone, init command creates this file
- [x] add device info into activity table (is only encoded in the fileid)
- [x] add device to query
- [x] fix fit parsing problems encountered when reading some older fenix5 files
  - were concatenated fit files where the latter contain HrMsg
- figure out how to decode HrMsg and update records
- import strava export
- web server and leaflet map view (same features, just web)
- publish to strava + tracking/storing the link
- scala3
- document a bit
- encoding (make fit files from db model)

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
