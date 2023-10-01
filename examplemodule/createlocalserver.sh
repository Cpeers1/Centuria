#!/bin/bash
if [ "$git" == "" ]; then
	git="https://github.com/Cpeers1/Centuria.git"
fi

dir="$(pwd)"

echo 'Updating standalone installation for testing...'

echo Cloning git repository...
tmpdir="/tmp/build-centuria-standalone-module/$(date "+%s-%N")"
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
    chmod +x gradlew
    mkdir deps
    git clone https://github.com/SkySwimmer/connective-http deps/connective-http
    ./gradlew installation || return $?
    if [ ! -d "$dir/server" ]; then
        mkdir "$dir/server"
    fi
    cp -rf "build/Installations/." "$dir/server"
    cp "build/Installations/Centuria.jar" "$dir/server/libs"

    if [ ! -d "$dir/libraries" ]; then
        mkdir "$dir/libraries"
    fi
    if [ ! -d "$dir/emulibs" ] ; then
        mkdir "$dir/emulibs"
    fi
    cp -rf "$dir/server/libs/." "$dir/libraries"
    cp -rf "$dir/server/libs/." "$dir/emulibs"
}

echo Building...
execute
exitmeth $?
