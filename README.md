# mvn-skip-bad-mirror

A maven 3.x extension to skip bad mirror matched by regex pattern, and repositories wht urls that also match the same pattern.

Intention is for scenarios described here, when your local development machine is on vpn, and maven is configured to use private central mirror, everything works smoothly.
Once the machine is disconnected from VPN and back to internet directly, the configured mirror wont be reachable anymore, this extension is to remove the mirror that is 
unreachable and matches an url pattern. It also go ahead remove any repositories configure for the project which maches the pattern too.

## example configuraiton
Put the extention jar under`$MAVEN_HOME/lib/ext`.

Create `extensions.xml` like following under `~/.m2` directory:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
    <extension>
        <groupId>com.github.af6140.mvn</groupId>
        <artifactId>mvn-skip-bad-mirror</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </extension>
</extensions>
```
