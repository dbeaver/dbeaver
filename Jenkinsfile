String str = BRANCH_NAME == "master" ? "0 0 * * *" : ""
String date = params.DATE ?: new Date().format('yyyyMMddHHmmss')

pipeline{
    agent {
        label "dbeaver-package"
    }
    triggers {
        cron(str)
    }
    options {
        ansiColor('xterm')
        disableConcurrentBuilds()
        timestamps()
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    environment {
        _pkg_path = '${WORKSPACE}/product/community/target/products/org.jkiss.dbeaver.core.product'
        _file_browser_pwd = credentials('export-ssh-pwd')
        _export_path = "/root/export/solution/tools/dbeaver/daily/${BRANCH_NAME}"
        _date = "${date}"
        _zip_pkg = "dbeaver.zip"
        _merge_build = 'False'
    }
    stages {
        stage("build") {
            when {
                anyOf {
                    not {
                        anyOf {
                            branch "master"
                        }
                    }
                    anyOf {
                        triggeredBy 'UserIdCause'
                        triggeredBy 'TimerTrigger'
                    }
                }
            }
            steps {
                script {
                    cleanWs()
                    checkout scm
                    sh "source /etc/profile && mvn clean package -o"
                }
            }
        }
        stage("check doc") {
            when {
                not {
                    anyOf {
                        branch "master"
                    }
                }
            }
            steps {
                script {
                    _merge_build = 'True'
                    sh "git clone git@git.yasdb.com:cod/yashandoc.git"
                    sh "python3 yashandoc/doccheck/main.py check doc/"
                }
            }
        }
        stage("Packing") {
            when {
                allOf {
                    anyOf {
                        branch "master"
                    }
                    anyOf {
                        triggeredBy 'UserIdCause'
                        triggeredBy 'TimerTrigger'
                    }
                }
            }
            steps {
                script {
                    _commit_id = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    sh '''cd ${WORKSPACE}/product/community/target/products/org.jkiss.dbeaver.core.product && zip -r dbeaver.zip linux win32'''
                    sh("sshpass -p ${_file_browser_pwd} ssh root@192.168.19.121 'mkdir -p ${_export_path}/latest' ")
                    sh("sshpass -p ${_file_browser_pwd} ssh root@192.168.19.121 'rm -rf ${_export_path}/latest/dbeaver*' ")
                    sh("sshpass -p ${_file_browser_pwd} ssh root@192.168.19.121 'mkdir -p ${_export_path}/${_date}' ")
                    sh("sshpass -p ${_file_browser_pwd} scp ${_pkg_path}/${_zip_pkg} root@192.168.19.121:${_export_path}/${_date}")
                    sh("sshpass -p ${_file_browser_pwd} ssh root@192.168.19.121 'cp ${_export_path}/${_date}/${_zip_pkg} ${_export_path}/latest/' ")
                }
            }
        }
	}
	post {
	    always {
            findText(
                textFinders: [
                    textFinder(
                        alsoCheckConsoleOutput: true,
                        regexp: '(Build|Install|Test)\\s[a-zA-Z-]+[\\s[a-zA-Z]+\\s]?failed|make.*:.+\\sError\\s[0-9]+|Test Run Error!|error: unknown type name|Segmentation fault|error: could not compile|compilation terminated|configure: error:|command not found|警告！文档错误|check fail!'
                    )
                ]
            )
        }
        failure {
    	    updateGitlabCommitStatus name: 'build', state: 'failed'
    	}
    	success {
            updateGitlabCommitStatus name: 'build', state: 'success'
            script {
                if (_merge_build != 'True') {
                    if ("${BRANCH_NAME}" == "master") {
                        build wait: false, propagate: false, job: 'document-build', parameters: [string(name: 'project', value: 'DBeaver'), string(name: 'branch', value: "master"), string(name: 'version', value: '23.1')]
                    }
                }
            }
    	}
    }
}
