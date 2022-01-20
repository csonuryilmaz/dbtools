#!/usr/bin/env groovy

import groovy.json.JsonSlurper

void checkDependency(String command) {
    def process
    println("⚙️ Check ${command} dependency ...")
    
    def mysqlCmd = "${command} --version"
    try {
        process = mysqlCmd.execute()
        process.waitForProcessOutput(System.out, System.err)
    } catch (e) {
        println(e.getMessage())
    }

    if (process?.exitValue() !=  0) {
        println("✖️ Couldn't  find ${command} utilities! Check whether ${command} command is in PATH and executable.")
        System.exit(1)
    }
    println('✔️ Success.')
}

checkDependency('mysql')
checkDependency('mysqldump')

println('⚙️ Parse config JSON ...')
def configJson
try {
    configJson = new JsonSlurper().parseText(new File('config.json').text)
} catch (e) {
    println(e.getMessage())
    println('✖️ Config JSON couldn\'t be parsed!')
    System.exit(2)
}

void checkPort(String side, def dbPort) {
    int port
    if (dbPort instanceof String) {
        try {
            port = dbPort.toInteger()
        } catch (e) {
            println(e.getClass().toString() + ' ' + e.getMessage())
            println("✖️ Port for ${side} database connection must be an integer value up to 65535. For example, 3306.")
            System.exit(2)
        }
    } else {
        port = dbPort
    }
    
    if (port < 1 || port > 65535) {
        println("✖️ Port for ${side} database connection must be an integer value up to 65535. For example, 3306.")
        System.exit(2)
    }
}

checkPort('source', configJson.source_db_connection_info.port)
checkPort('destination', configJson.destination_db_connection_info.port )

println('✔️ Success.')
