String date = params.DATE ?: new Date().format('yyyyMMddHHmmss')

pipeline{
    agent{
        label "dbeaver-package"
    }
    options{
        ansiColor('xterm')
        disableConcurrentBuilds()
        timestamps()
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr:'10'))
    }
    environment {
      _pkg_path = '${WORKSPACE}/product/community/target/products/org.jkiss.dbeaver.core.product'
      _file_browser_pwd = credentials('export-ssh-pwd')
      _export_path = "/root/export/solution/tools/dbeaver/daily/${BRANCH_NAME}"
      _date = "${date}"
      _zip_pkg = "dbeaver.zip"
    }
    stages{
        stage("build"){
            steps{
                cleanWs()
                checkout scm
                script{
                	sh "source /etc/profile && mvn clean package -o"
                }
            }
        }
        stage("Packing"){
            steps{
                script{
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
	post{
	    always {
            findText(
                textFinders: [
                    textFinder(
                        alsoCheckConsoleOutput: true,
                        regexp: '(Build|Install|Test)\\s[a-zA-Z-]+[\\s[a-zA-Z]+\\s]?failed|make.*:.+\\sError\\s[0-9]+|Test Run Error!|error: unknown type name|Segmentation fault|error: could not compile|compilation terminated|configure: error:|command not found'
                    )
                ]
            )
        }
        failure {
    	    updateGitlabCommitStatus name: 'build',state: 'failed'
    	}
    	success {
            updateGitlabCommitStatus name: 'build',state: 'success'
    	}
    }
}
