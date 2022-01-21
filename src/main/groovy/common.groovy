import groovy.json.JsonSlurper

void terminateWithMessage(String msg) {
    println('✖️ ' + msg)
    System.exit(1)
}

void runCommand(String command, String failMsg) {
    def process
    try {
        process = command.execute()
        process.waitForProcessOutput(System.out, System.err)
    } catch (e) {
        println(e.getMessage())
    }

    if (process?.exitValue() !=  0) {
        terminateWithMessage(failMsg)
    }
}

void mysqldumpShouldExist() {
    def mysqldumpCmd = 'mysqldump --version'
    runCommand(mysqldumpCmd, 'Could not find mysqldump utilities! Check whether mysqldump command is in PATH and executable.')
}

def configJsonShouldBeParseable() {
    def configJson
    try {
        configJson = new JsonSlurper().parseText(new File('exportdb.json').text)
        
        if (!configJson.connection) {
            terminateWithMessage('Could not find connection section in JSON arguments. (exportdb.json->connection)')
        }
    } catch (e) {
        println(e.getMessage())
        terminateWithMessage('Config JSON could not be read or parsed!')
    }
    
    return configJson
}

void userCredentialsShouldBeFilled(def configJson){
    String info = 'Username and password must be set in JSON arguments. (exportdb.json->connection)'

    if (!configJson.connection.username) {
        println('Could not find username in JSON arguments.')
        terminateWithMessage(info)
    }
    if (!configJson.connection.password) {
        println('Could not find password in JSON arguments.')
        terminateWithMessage(info)
    }
}

void serverArgumentsShouldBeFilled(def configJson){
    String info = 'Host and database must be set in JSON arguments. (exportdb.json->connection)'
    
    if (!configJson.connection.host) {
        println('Could not find host in JSON arguments.')
        terminateWithMessage(info)
    }
    if (!configJson.connection.database) {
        println('Could not find database in JSON arguments.')
        terminateWithMessage(info)
    }
}

void connectionPortShouldBeInRange(def configJson){
    String info = 'Port for database connection must be an integer value up to 65535. For example, 3306.'
    
    if (!configJson.connection.port) {
        println('Could not find port in JSON arguments. (exportdb.json->connection)')
        terminateWithMessage(info)
    }
    
    int port
    if (configJson.connection.port instanceof String) {
        try {
            port = configJson.connection.port.toInteger()
        } catch (e) {
            println(e.getClass().toString() + ' ' + e.getMessage())
            terminateWithMessage(info)
        }
    } else {
        port = configJson.connection.port
    }
    
    if (port < 1 || port > 65535) {
        println("${port} value is out of range")
        terminateWithMessage(info)
    }
}

void getUserPermissionIfOutputFileExists() {
    if (new File('schema.sql').exists()) {
        def answer =  System.console().readLine('Warn: It seems you have a schema.sql file in current working directory. Overwrite? (y/n) ')
        if (answer.length() == 0 || answer.toLowerCase().charAt(0) == 'n') {
            println('✖️ Terminated by user request.')
            System.exit(2)
        }
    }
}
