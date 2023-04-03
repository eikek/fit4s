# fit4s - encode and decode ANT FIT files with Scala

This is a library for Scala 2.13 and 3.

## TODO

- add LapMsg to database(?)
- scala3
- cleanup playground test mess
- summary on db
- nicer prints, remove lines with no value, etc
- have json and pretty output
- list activities, option to list files only 
- add/set/remove/rename tags
- update command
- [x] check path before adding location record
- config file with db data and timezone, init command creates this file
- add device info into activity table (is only encoded in the fileid)
- add device to query
- [x] fix fit parsing problems encountered when reading some older fenix5 files
  - were concatenated fit files where the latter contain HrMsg
- figure out how to decode HrMsg and update records
- import strava export
- web server and leaflet map view (same features, just web)
- publish to strava + tracking/storing the link
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
