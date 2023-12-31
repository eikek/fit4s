# fit4s - look at your ANT FIT files with Scala

This is a library for Scala 3 for decoding and (to some extend)
encoding FIT files.

On top of that, there is a CLI application for managing your fit files
locally, including a web frontend and optional integration into
Strava.


## Quickstart

1. Download the zip archive from the release page and unzip it
   somewhere. Add the `bin/` folder to your `$PATH`. For the
   remainder, I symlinked the `fit4s-cli` to be just `fit4s`.
2. Initialize the database with:
   ```
   fit4s init
   ```
3. Add your folder of `fit` files:
   ```
   fit4s activity import /folder/to/your/fit-files/
   ```
4. View things via the cli, for example:
   ```
   fit4s activity summary
   fit4s activity list --week
   ```
5. View things via a browser:
   ```
   fit4s server start
   ```
   And go to `http://localhost:8181`.

Checkout the possibilities via the `--help` option. Every command and
subcommand provides a short help of possible options.

Configuring is done via environment variables. It is recommended to
create some wrapper script that sets variables and then calls the cli.
Use `fit4s config list-defaults` to see the default configuration. Use
`fit4s config show-current` to see the currently applied values.

As a database, [H2](https://www.h2database.com) is used by default
which is a fast in-process database that writes directly to the
filesystem. [PostgreSQL](https://postgresql.com) is also supported by
setting the apropriate JDBC url (using `FIT4S_DB_URL`).

This project targets FIT files only, but out of desperate need it also
imports activities from TCX files… :).


## Modules

### core

The core module is a library for decoding (and to some extend
encoding) FIT files. The codecs are written using
[scodec](https://github.com/scodec/scodec).

Here is a very quick overview of a FIT file: A FIT file consists of

1. file header (min. 12 bytes, preferred 14 bytes, maybe larger)
2. data records
3. CRC (2 bytes)

Data records contain the main content. Records have a 1-byte header
and a content and are split in two classes:

1. definition messages: define upcoming data messages
2. data messages: data fields in format described by preceding
   definition message

#### Decoding

Use `FitFile.decode` to decode a `ByteVector` into a sequence of FIT
messages.

Each data message carries the most recent definition message with it,
so it is not necessary to pair them up. Select the desired data
message and decode individual fields using
`dataMessage.getField(MessageType.fieldName)`. `MessageType` are
predefined objects with their set of fields (that have been generated
from the sdk profile).

For convenience, there is something prepared for decoding activities,
look at `ActivityReader`. More can be created in a similar way.

#### Encoding

Encoding is only supported for a prepared set of FIT messages that
make up a `FitFile`. Instances of `FitFile` can be encoded into binary
using the provided codec.

### activities

This module builds on `core` and supporting modules to provide
functionality for managing activities from fit files. The entry point
is `ActivityLog`.

The basic idea is to read in FIT files into a database structure to be
able to get some insights.

Additionally it supports integrating with Strava and does reverse
geocoding of activity start and end positions (using the modules
`geocode` and `strava`).


### geocode

Using the nominatim reverse webservice from OSM, looks up places given a geo coordinate pair.


### strava

Implements a minimal strava client, just enough to support
functionality for the `activities` module.


### webview

This module provides a web client. It consists of a http server and a
browser frontend written in ScalaJS. It is integrated into the cli,
that allows to start the server.


### cli

This module contains a command line application to manage your
activities.

The basic use case is this:
- keep your fit files from your devices on your machine (I rsync them
  to some folder)
- import them into a (in process) database
- associate tags, search and look at summaries
- optionally, create an app on Strava and link/upload local activities
  to strava
- import a strava export archive
  
You can find a pre-build zip file in the release section. To get
started, run the cli without anything to see what's available. The
first thing required is to run `init` to initialize the database. Then
`import` some fit files and look at them using `list` or `summary`.
The cli supports a simple query to search arbitrarily in the
activties.

When new files come, run `update` to import new files.

When syncing with Strava, tags starting with `Bike/` and `Shoe/`,
respectively, are used to associate Strava gear to activities. The tag
`Commute` is used to mark activites as commute when uploading to
strava. This works the other way around as well, when linking
activities from Strava to existing local activities. Activities tagged
with `No-Strava` are discarded. These are defaults that can be changed
on the respective commands.


## Using the query

When listing activities a query can be specified. The following shows
a list of possible conditions to specify. Any condition can be
combined with `and`, `or` or negated.

AND:
```
(and <cond1> ... <condN>)
(& <cond1> ... <condN>)
```

OR:
```
(or <cond1> ... <cond2>)
(| <cond1> ... <cond2>)
```

Negate: prefix a condition with a `!`, like `!tag=Tag1`

When values contain whitespace or some special chars, wrap it in quotes.

### tags

Search for tags that match a prefix or the whole name. 

```
tag=Tag1
```

Multiple tags can be given via comma sparared or plus separated list.
The first means any match, the latter all match. 

An activity must have both tags:

```
tag=Tag1+Tag2
```

Here one of the tags is enough:
```
tag=Tag1,Tag2
```

Using the `=` means to compare the whole tag name. Using a `~` meanst
to match a prefix. For example:

```
tag~Bike/
```

Tags can contain whitespace and some special chars, except `,+%`.

### location

The directories that have been imported can be used in a query, analogous to tags.

```
location=/some/dir
```

it also supports multiple values using comma or plus to separate them.
Instead of `location`, the shorter variant `loc` can be used.


### file_id

The `file_id` is a string representation of the `FileId` message that
must be in every activtiy file. It is a unique stable identifier and
is also used to detect duplicates. The file id is printed when looking
at activity details.

```
file_id=Abc123
```

### id

This is the primary database identifier of an activity. It's easier to
type than the `filev_id` but is not stable when re-importing the same
files.

```
id=65
```

### sport and sub-sport

Checks for fit sport and sub-sport enumerated values.

```
sport=cycling
```

```
sub-sport=indoor_cycling
```

Available values are printed when using a wrong one :).


### start time

Check for started time using `started>` or `started<` followed by a
date time value. The date time can be given in various ways:

- `<n>days|d` for `n` days back, like `7d` 
- `<n>week|weeks` for `n` weeks back, like `4weeks`
- a partial timestamp, like `2022-10` or even `2022` - all missing
  parts are filled to be the minimal value.
- Epoch seconds, like `1670674515`
  
Datetime values are treated in the configured timezone or a final `Z`
can be used to specify UTC.

### distance

Check for distance using `distance>` or `distance<`. Distances are given in `<n>k|km|m`.

```
distance>150k
```

### elapsed and moved time

Can be checked via `elapsed>|<` and `moved>|<`. Duration values are
given in minutes or hours, using unit abbreviations. Example:

```
elapsed<6h
moved>68min
```


### device

You can check for your device name using `device=<name>`, like `device=fenix5`.


### activity name and notes

Name and notes of an activity can be searched.

```
name="my name"
notes="contains this"
```

Name and notes are checked for containing the given value.

### strava link

You can search activities having a associated strava id or not.

```
strava-link:yes|no
```


## Tech Stack

The core library only implements the FIT codec using the scodec
library. The CLI application uses more libraries:

- [Scala 3](https://scala-lang.org) all the way, [Scala.js](https://www.scala-js.org/) for the web frontend
- [scodec](https://github.com/scodec/scodec) for writing the FIT encoder/decoder
- based on [cats-effect](https://github.com/typelevel/cats-effect) and [fs2](https://github.com/typelevel/fs2)
- [doobie](https://github.com/tpolecat/doobie) for DB access
- [http4s](https://github.com/http4s/http4s) for the http api
- [decline](https://github.com/bkirwi/decline) for parsing cli options
- [ciris](https://github.com/vlovgr/ciris) for configuration
- [borer](https://github.com/sirthias/borer) for JSON encoding/decoding
- [scribe](https://github.com/outr/scribe) for logging
- [calico](https://github.com/armanbilge/calico) for Scala.js based web application
- [tailwind](https://tailwindcss.com/) for styling (css)


## Development

This is a Scala 3 project. For development, install `npm` and `sbt` or
use the provided nix setup. You can drop into a shell with `nix
develop`.

Run `sbt compile` to compile the whole project.

For developing the webclient with ScalaJS, use two terminals. The
first runs sbt, where first the http server is started and then the
watch command runs to build the javascript using ScalaJS. The second
terminal runs `npm` that will react on newly build js files and runs
the frontend build.

Terminal 1:
```
> sbt
…
sbt:fit4s-root> cli/reStart server start
…
cli 2023.07.25 19:55:06:095 io-compute-11 INFO org.http4s.ember.server.EmberServerBuilderCompanionPlatform
cli     Ember-Server service bound to address: 127.0.0.1:8181
cli Started webview server at 127.0.0.1:8181
sbt:fit4s-root> ~webviewJS/fastLinkJS
…
[info] 1. Monitoring source files for root/webviewJS/fastLinkJS...
[info]    Press <enter> to interrupt or '?' for more options.
```

Terminal 2:
```
> cd modules/webview
> npm run dev

  VITE v4.3.9  ready in 23342 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ➜  press h to show help
```

If a scala file in `modules/webview/{js,shared}` is changed, the first
terminal compiles a new javascript output and the second terminal
builds a new version of the frontend.

The http api is at `localhost:8181` and the frontend at
`localhost:5173`.

This project started out from the tutorial for [ScalaJS and
Vite](https://www.scala-js.org/doc/tutorial/scalajs-vite.html).
