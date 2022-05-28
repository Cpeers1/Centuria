#!/bin/bash

server="https://aerialworks.ddns.net/extra/emuferal"
source version.info

base="$server/$channel"

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

echo
echo Upload completed.
