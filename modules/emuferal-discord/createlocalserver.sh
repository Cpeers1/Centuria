#!/bin/bash
if [ "$git" == "" ]; then
	git="https://github.com/SkySwimmer/EmuFeral.git"
fi

dir="$(pwd)"

echo 'Updating standalone installation for testing...'

echo Cloning git repository...
tmpdir="/tmp/build-emuferal-standalone-module/$(date "+%s-%N")"
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
    ./gradlew installation || return $?
    if [ ! -d "$dir/server" ]; then
        mkdir "$dir/server"
    fi
    cp -rf "build/Installations/." "$dir/server"
    cp -rf "libraries/"*-javadoc.jar "$dir/server/libs"
    cp -rf "libraries/"*-sources.jar "$dir/server/libs"
	cp "build/Installations/EmuFeral.jar" "$dir/server/libs"
    
    if [ ! -d "$dir/libraries" ]; then
        mkdir "$dir/libraries"
    fi
    cp -rf "$dir/server/libs/." "$dir/libraries"
}

echo Building...
execute
exitmeth $?
