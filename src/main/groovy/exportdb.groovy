#!/usr/bin/env groovy

GroovyShell shell = new GroovyShell()
def common = shell.parse(new File('src/main/groovy/common.groovy'))

println()
println('⚙️ Check mysqldump ...')
common.mysqldumpShouldExist()
println('✔️ Success.')

println()
println('⚙️ Parse config JSON ...')
def configJson = common.configJsonShouldBeParseable('exportdb.json')
common.userCredentialsShouldBeFilled(configJson)
common.serverArgumentsShouldBeFilled(configJson)
common.connectionPortShouldBeInRange(configJson)
println('✔️ Success.')

println()
println('⚙️ Execute mysqldump ...')

String mysqldumpCmd = 'mysqldump --force --single-transaction --routines --triggers --events --no-data'

for (argument in configJson.additional_arguments) {
    argument = " " + argument.trim()
    if (argument) {
        mysqldumpCmd += argument
    }
}

mysqldumpCmd += " --host=${configJson.connection.host.trim()}"
mysqldumpCmd += " --port=${configJson.connection.port}"
mysqldumpCmd += " --user=${configJson.connection.username.trim()}"
mysqldumpCmd += " --password=${configJson.connection.password.trim()}"

if (configJson.excluded_tables) {
    int ignoredCnt
    for (table in configJson.excluded_tables) {
        table = table.trim()
        if (table) {
            mysqldumpCmd += " --ignore-table=${configJson.connection.database.trim()}.${table}"
            ignoredCnt++
        }
    }
    println("Info: ${ignoredCnt} table(s) will be excluded from database export.")
}

mysqldumpCmd += " --result-file=schema.sql"

mysqldumpCmd += " " + configJson.connection.database.trim()

common.getUserPermissionIfOutputFileExists()
common.truncateOutputFileIfExists()

println('Info: You can also track export process from SQL file. For example, tail -f schema.sql')

println()
println(mysqldumpCmd)
println()

println('Please wait while dumping in progress ...')
println()

exitCode = common.runCommand(mysqldumpCmd)
if (exitCode == 0) { // success
    println('✔️ Mysqldump successfully exported database to schema.sql file.')    
} else {
    if (common.outputFileHasAnyCreateTableStatement()) { // warning
        println('⚠️ Mysqldump has some warnings but it may be ok. Check messages and schema.sql file.')
    } else { // fatal error
        println('✖️ Mysqldump exit with error code. Check error messages and try again.')
        System.exit(exitCode)
    }
}
