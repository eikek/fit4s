#!/usr/bin/env bash

# build the application, which is a bit involved.
#
# first, just compile to catch any errors and have next steps run a
# bit quicker. Then build the webclient, which is wrapped into a
# webjar and put into the resources of the webview jvm part so it can
# be served from there. At last the cli is build.
#
# requires: sbt, npm, java (or run via nix)

set -o errexit -o pipefail -o noclobber -o nounset

export SBT_OPTS="-Xmx2G"

tdir="$(pwd)"
wdir=$(realpath "$(dirname "$0")")
cd $wdir

# compile the project
sbt clean compile

# create the webclient
cd $wdir/modules/webview
export FIT4S_BUILD_PROD=true
npm install && npm run build

# create the webjar
echo "Create webjar ..."
rm -rf fit4s-webview-client.jar webjar/
mkdir -p webjar/META-INF/resources/webjars/fit4s-webview
cp -r dist/* webjar/META-INF/resources/webjars/fit4s-webview/
cp node_modules/leaflet/dist/images/* webjar/META-INF/resources/webjars/fit4s-webview/
cd webjar
jar -c --file fit4s-webview-client.jar *
mv fit4s-webview-client.jar ..
cd $wdir/modules/webview
rm -rf webjar

# add the webjar as dependency and build the cli
echo "Build the CLI"
mkdir -p jvm/lib/
cp fit4s-webview-client.jar jvm/lib/

cd "$wdir"
sbt make-package
cd "$tdir"
echo "Done"
