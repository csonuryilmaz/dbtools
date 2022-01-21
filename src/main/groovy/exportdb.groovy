#!/usr/bin/env groovy

GroovyShell shell = new GroovyShell()
def common = shell.parse(new File('src/main/groovy/common.groovy'))

println()
println('⚙️ Check mysqldump ...')
common.mysqldumpShouldExist()
println('✔️ Success.')

println()
println('⚙️ Parse config JSON ...')
def configJson = common.configJsonShouldBeParseable()
common.userCredentialsShouldBeFilled(configJson)
common.serverArgumentsShouldBeFilled(configJson)
common.connectionPortShouldBeInRange(configJson)
println('✔️ Success.')

println()
println('⚙️ Execute mysqldump ...')

String mysqldumpCmd = 'mysqldump --force --single-transaction --routines --triggers --events --no-data'
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

println('Info: You can also track export process from SQL file. For example, tail -f schema.sql')

println()
println(mysqldumpCmd)
println()

println('Please wait while dumping in progress ...')
println()
common.runCommand(mysqldumpCmd, 'Mysqldump exit with error code, please check mysqldump error messages and try again.')

println('✔️ Success.')
