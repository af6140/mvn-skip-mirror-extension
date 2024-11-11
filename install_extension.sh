#!/usr/bin/env zsh
#set -x
mvn_des=$(mvn -v | grep "Maven home" | awk -F ':' '{print $2}')
#remove leading space
mvn_des="${mvn_des##* }"
dest="${mvn_des}/lib/ext/"
echo "Destination :${dest}"
if test -d "${dest}"; then
  echo "Copying file to destination"
  cp target/mvn-skip-bad-mirror-1.0.0-SNAPSHOT.jar $dest
fi
