#!/usr/bin/env groovy
def log_file = new File('target/it/simple/build.log')
println("Checking log file ")
log_file.eachLine { line ->
    if (line.contains('Removing invalid or unreachable mirror')) {
        println('Found target string')
        System.exit(0)
    }
}
System.exit(-1)
