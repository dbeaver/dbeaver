String str = BRANCH_NAME == "master" ? "0 0 * * *" : ""
String date = params.DATE ?: new Date().format('yyyyMMddHHmmss')

pipeline{
    agent {
        kubernetes {
            cloud 'kubernetes-new'
            yamlFile 'env.yaml'
            workspaceVolume dynamicPVC(accessModes: 'ReadWriteMany', storageClassName: 'centos-test-storage')
        }
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
        _file_browser_pwd = credentials('jenkins-ssh-pwd')
        _export_path = "/data/cod/pkg_release/tools/dbeaver/daily/${BRANCH_NAME}"
        _date = "${date}"
        _version = "23.1"
        _merge_build = 'False'
        _zip_pkg = "DBeaver-For-YashanDB*.zip"
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
                    sh "source /etc/profile && mvn clean package -Pall-platforms -o"
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
                    sh("sh package.sh ${_version} ${_zip_pkg}")
                    sh("sshpass -p ${_file_browser_pwd} ssh -o StrictHostKeyChecking=no jenkins@192.168.29.112 'mkdir -p ${_export_path}/latest' ")
                    sh("sshpass -p ${_file_browser_pwd} ssh -o StrictHostKeyChecking=no jenkins@192.168.29.112 'rm -rf ${_export_path}/latest/DBeaver*' ")
                    sh("sshpass -p ${_file_browser_pwd} ssh -o StrictHostKeyChecking=no jenkins@192.168.29.112 'mkdir -p ${_export_path}/${_date}' ")
                    sh("sshpass -p ${_file_browser_pwd} scp -o StrictHostKeyChecking=no ${_pkg_path}/${_zip_pkg} jenkins@192.168.29.112:${_export_path}/${_date}")
                    sh("sshpass -p ${_file_browser_pwd} ssh -o StrictHostKeyChecking=no jenkins@192.168.29.112 'cp ${_export_path}/${_date}/${_zip_pkg} ${_export_path}/latest/' ")
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
                        build wait: false, propagate: false, job: 'document-build', parameters: [string(name: 'project', value: 'DBeaver'), string(name: 'branch', value: "master"), string(name: 'version', value: '23.1'), string(name: 'doc_dir', value: "${_export_path}/latest/")]
                    }
                }
            }
    	}
    }
}
