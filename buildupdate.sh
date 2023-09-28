#!/bin/bash

function copyUpdateData() {
	version="$1"
	channel="$2"
	
	# Create directory
    mkdir -p "build/updatedata/emuferal.ddns.net/httpdocs/$channel" || exit 1
	
	# Copy files
	function copyData() {
		for file in $1/* ; do
			pref=$2
			targetf="${file:${#pref}}"
			if [ -f "$file" ]; then
				dir="$(dirname "build/updatedata/emuferal.ddns.net/httpdocs/$channel/${version}/${targetf}")"
				if [ ! -d "$dir" ]; then
					mkdir -p "$dir" || exit 1
				fi
				cp -rfv "$file" "build/updatedata/emuferal.ddns.net/httpdocs/$channel/${version}/${targetf}"
			fi
			if [ "$?" != "0" ]; then
				echo Build failure!
				exit 1
			fi
			if [ -d "$file" ]; then
				copyData "$file" "$2"
			fi
		done
	}
	
	copyData build/update build/update/
	echo -n "$version" > "build/updatedata/emuferal.ddns.net/httpdocs/$channel/update.info"

	source version.info
}

# Current channel
echo Preparing...
source version.info
rm -rf build/updatedata
rm -rf build/update
echo
echo
echo Centuria Update Builder
echo Version: $version
echo Version type: $channel
echo
echo
read -p "Are you sure you want to build this version's update files? [Y/n] " prompt

if [ "$prompt" != "y" ] && [ "$prompt" != "Y" ]; then
	exit
fi

echo Building centuria...
./gradlew build updateData || exit $?
echo

echo Copying data...
copyUpdateData "$version" "$channel"

# Other channels
if [ "$channel" == "beta" ]; then
	copyUpdateData "$version" alpha
fi
if [ "$channel" == "prerelease" ]; then
	copyUpdateData "$version" alpha
	copyUpdateData "$version" beta
fi
if [ "$channel" == "release" ]; then
	copyUpdateData "$version" alpha
	copyUpdateData "$version" beta
	copyUpdateData "$version" prerelease
fi

echo
echo Build completed.
