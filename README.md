# fit4s - encode and decode ANT FIT files with Scala

This is a library for Scala 2.13 and 3.

## TODO

- [x] better error when init was not called, or just do initialize
  - not really possible without perf impact by checking each time
    another command is used
  - well, could catch the exception and print something better
  - this is already ok enough when the printStacktrace is gone
- [x] have json and pretty output
- [x] update files from known locations
- [x] list activities, option to list files only 
- [ ] import strava export, tracking/storing the link to strava for each activity
  - if activity exists, add tags and other additional information
  - ask to overwrite activity name
- [ ] web server and leaflet map view (same features, just web, scalajs)
- [ ] publish to strava 
- [x] add/remove/rename tags
- [x] update command
- [x] add LapMsg to database
- [ ] cleanup playground test mess
- [ ] reverse location lookup?
  - https://nominatim.openstreetmap.org/reverse?format=json&lat=30.4573699&lon=-97.8247654
  - it is allowed to do 1req/s
  - can first look into the local database and cache results
  - is enough to have +-2000 semicircle accuracy
- [ ] mima for the libraries
- [x] config file with db data and timezone, init command creates this file
  - decided against a config file, just use env vars it's only a few values
- [x] fix invalid gps values showing up as valid
- [x] fix missing session values using corresponding records
- [x] add tags to import cmd
- [x] parallel directory import (max(3, cpus - 2))
- [x] try fix timestamps for GarminSwim
  - not possible to get the correct time, bc it is based on "seconds
    from device power on" (when batteries got inserted). Since
    batteries have been changed I can't know the correct value)
  - The activity contains a local timestamp, this made sense for a
    file, so I'm using this as base to correct all values that are
    below the minimum
- [x] summary on db
- [x] nicer prints, remove lines with no value, etc
- [x] check path before adding location record
- [x] add device info into activity table (is only encoded in the fileid)
- [x] add device to query
- [x] fix fit parsing problems encountered when reading some older fenix5 files
  - were concatenated fit files where the latter contain HrMsg
- [ ] catch logging from java.util.logging
- [ ] remove hr data from fit files (so to upload without this data in them)
- [ ] figure out how to decode HrMsg (reconstruct timestamps) and update records
- [ ] scala3
- [ ] document a bit
- [ ] encoding (make fit files from db model)

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
