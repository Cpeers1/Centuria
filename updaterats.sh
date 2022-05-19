#!/bin/bash
git="https://aerialworks.ddns.net/ASF/RATS.git"
dir="$(pwd)"

echo 'Updating RaTs! installation for libraries...'
rm -rf libraries/ 2>/dev/null

echo Cloning git repository...
tmpdir="/tmp/build-rats-connective-http-standalone/$(date "+%s-%N")"
rm -rf "$tmpdir"
mkdir -p "$tmpdir"
git clone $git "$tmpdir"
cd "$tmpdir"
echo

function exitmeth() {
    cd "$dir"
    rm -rf "$tmpdir"
    echo
    exit $1
}

function execute() {
    ./gradlew installation || return $?
    
    if [ ! -d "$dir/libraries" ]; then mkdir "$dir/libraries"; fi
    cp -r "build/Installations/"*.jar "$dir/libraries"
}

echo Building...
execute
exitmeth $?
