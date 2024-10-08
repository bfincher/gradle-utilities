def performRelease = false
def baseNexusUrl = "http://nexus3:8081"
def localNexus = "${baseNexusUrl}/repository/public"
def gradleOpts = "-s --build-cache -PlocalNexus=${localNexus}"
def buildCacheDir = ""
def buildCacheSymlink = "/tmp/buildCache"
def gradleCmd = 'gradle'

pipeline {
  agent { label 'gradle-8.10-jdk11' }

  parameters {
    string(defaultValue: '', description: 'Extra Gradle Options', name: 'extraGradleOpts')
    booleanParam(name: 'majorRelease', defaultValue: false, description: 'Perform a major release')
    booleanParam(name: 'minorRelease', defaultValue: false, description: 'Perform a minor release')
    booleanParam(name: 'patchRelease', defaultValue: false, description: 'Perform a patch release')
    booleanParam(name: 'publish', defaultValue: false, description: 'Publish to nexus')
    string(name: 'baseBuildCacheDir', defaultValue: '/cache', description: 'Base build cache dir')
    string(name: 'buildCacheName', defaultValue: 'default', description: 'Build cache name')
  }

  environment {
    GRADLE_USER_HOME = "${buildCacheSymlink}"
  }

  stages {
    stage('Prepare') {
      steps {
        script {
          
          sh "wget ${localNexus}/com/fincher/gradle-cache/0.0.1/gradle-cache-0.0.1.tgz -O /tmp/gradle-cache.tgz"
          sh "tar -zxf /tmp/gradle-cache.tgz --directory /tmp"

          buildCacheDir = sh(
              script: "/tmp/getBuildCache ${params.baseBuildCacheDir} ${params.buildCacheName}",
              returnStdout: true).trim()

          sh """
            if [ -e ${buildCacheSymlink} ]
            then
              rm ${buildCacheSymlink}
            fi

            ln -s ${buildCacheDir} ${buildCacheSymlink}
          """

          def releaseOptionCount = 0;
          def prepareReleaseOptions = "";
          
          if (params.majorRelease) {
            performRelease = true
            prepareReleaseOptions = "--releaseType MAJOR"
            releaseOptionCount++
          }
          if (params.minorRelease) {
            performRelease = true
            prepareReleaseOptions = "--releaseType MINOR"
            releaseOptionCount++
          }
          if (params.patchRelease) {
            performRelease = true
            prepareReleaseOptions = "--releaseType PATCH"
            releaseOptionCount++
          }

          if (releaseOptionCount > 1) {
            error("Only one of major, minor, or patch release options can be selected")
          }                         
  
          if (!params.extraGradleOpts.isEmpty()) {
            gradleOpts = gradleOpts + " " + extraGradleOpts
          }

          sh "git config --global user.email 'brian@fincherhome.com' && git config --global user.name 'Brian Fincher'"

          if (performRelease) {
            sh "$gradleCmd prepareRelease $prepareReleaseOptions $gradleOpts"
          }
        }
      }
    }

    stage('Build') {
      steps {
        sh "$gradleCmd clean build $gradleOpts"
      }
    }
    
    stage('Finalize') {
      when { expression { performRelease || params.publish } }
      steps {
        script {
          def publishParams = '-PpublishUsername=${publishUsername} -PpublishPassword=${publishPassword}'
          publishParams += " -PpublishSnapshotUrl=${baseNexusUrl}/repository/snapshots"
          publishParams += " -PpublishReleaseUrl=${baseNexusUrl}/repository/releases"
          withCredentials([usernamePassword(credentialsId: 'nexus.fincherhome.com', usernameVariable: 'publishUsername', passwordVariable: 'publishPassword')]) {
            sh "${gradleCmd} publish ${publishParams} ${gradleOpts}" 
          }

          if (performRelease) {
            withCredentials([sshUserPrivateKey(credentialsId: "bfincher_git_private_key", keyFileVariable: 'keyfile')]) {
              sh "${gradleCmd} finalizeRelease -PsshKeyFile=${keyfile} ${gradleOpts}"
            }
          }
          
        }
      }
    }
  }

  post {
    always {
      sh("/tmp/releaseBuildCache ${buildCacheDir}")
    }
  }
}

