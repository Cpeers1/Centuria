#!/bin/bash
chmod +x "$0"
dirpath="$(dirname "$0")"

libs=$(find "$dirpath/libs/" -name '*.jar' -exec echo -n :{} \;)
libs=${libs:1}
	
java -cp "$libs" "$@"
exit $?
