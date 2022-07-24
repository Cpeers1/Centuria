#!/bin/bash

server="https://aerialworks.ddns.net/extra/emuferal"

function uploadToServers() {
	version="$1"
	channel="$2"
	
	base="$server/$channel"
	
	function upload() {
		for file in $1/* ; do
			pref=$2
			target="${file:${#pref}}"
			echo curl -X PUT "$base/$version/$target" -u "REDACTED" --data-binary @"$file"
			curl -X PUT "$base/$version/$target" -u "$username:$password" --data-binary @"$file"
			if [ "$?" != "0" ]; then
				echo Upload failure!
				exit 1
			fi
			if [ -d "$file" ]; then
				upload "$file" "$2"
			fi
		done
	}
	
	upload build/update build/update/
	echo curl -X PUT "$base/update.info" -u "REDACTED" --data-binary "$version"
	curl -X PUT "$base/update.info" -u "$username:$password" --data-binary "$version"

	source version.info
}

# Current channel
source version.info
rm -rf build/Update
read -rp "Server username: " username
read -rsp "Server upload password: " password
echo
echo
echo
echo EmuFeral Uploader
echo Version: $version
echo Version type: $channel
echo
echo
read -p "Are you sure you want to upload this version to the server? [Y/n] " prompt

if [ "$prompt" != "y" ] && [ "$prompt" != "Y" ]; then
	exit
fi

echo Building emuferal...
./gradlew build updateData || exit $?
echo

echo Uploading data...
uploadToServers "$version" "$channel"

# Other channels
if [ "$channel" == "beta" ]; then
	uploadToServers "$version" alpha
fi
if [ "$channel" == "prerelease" ]; then
	uploadToServers "$version" alpha
	uploadToServers "$version" beta
fi
if [ "$channel" == "release" ]; then
	uploadToServers "$version" alpha
	uploadToServers "$version" beta
	uploadToServers "$version" prerelease
fi

echo
echo Upload completed.