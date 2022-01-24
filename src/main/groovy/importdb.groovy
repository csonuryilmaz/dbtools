#!/usr/bin/env groovy

GroovyShell shell = new GroovyShell()
def common = shell.parse(new File('src/main/groovy/common.groovy'))

println()
println('⚙️ Check mysql ...')
common.mysqlShouldExist()
println('✔️ Success.')

println()
println('⚙️ Parse config JSON ...')
def configJson = common.configJsonShouldBeParseable('importdb.json')
common.userCredentialsShouldBeFilled(configJson)
common.serverArgumentsShouldBeFilled(configJson)
common.connectionPortShouldBeInRange(configJson)
println('✔️ Success.')

println()
println('⚙️ Create database ...')

String genericFailMsg = 'Mysql exit with error code! Please check error messages and try again if needed.'

mysqlCmd = ['mysql','--force','--verbose','--connect-timeout=60']
if (!common.isOSWindows()) {
    mysqlCmd << '--skip-pager'
}
mysqlCmd << '--default-character-set=utf8mb4'
mysqlCmd << "--host=${configJson.connection.host.trim()}"
mysqlCmd << "--port=${configJson.connection.port}"
mysqlCmd << "--user=${configJson.connection.username.trim()}"
mysqlCmd << "--password=${configJson.connection.password.trim()}"

if (configJson.drop_database_if_exists) {
    dropDatabaseSQL = "drop database if exists ${configJson.connection.database.trim()};"
    dropDatabaseCmd =  mysqlCmd + '-e' + dropDatabaseSQL
    println()
    println(dropDatabaseCmd)
    println()
    common.runCommand(dropDatabaseCmd, genericFailMsg )
}

createDatabaseSQL = "CREATE DATABASE ${configJson.connection.database.trim()}"
if (configJson.character_set) {
    createDatabaseSQL += " CHARACTER SET ${configJson.character_set.trim()}"
}
if (configJson.collate) {
    createDatabaseSQL += " COLLATE ${configJson.collate.trim()}"
}
createDatabaseSQL += ";"
createDatabaseCmd = mysqlCmd + '-e' + createDatabaseSQL
println()
println(createDatabaseCmd)
println()
common.runCommand(createDatabaseCmd, genericFailMsg)
println('✔️ Success.')

println()
println('⚙️ Import database ...')

common.schemaSQLFileShouldExist()

mysqlCmd += "--database=${configJson.connection.database.trim()}"
mysqlCmd += "< schema.sql"

println()
println(mysqlCmd)
println()

common.runCommand(['bash', '-c',mysqlCmd.join(' ') ], genericFailMsg )

println('✔️ Success.')
