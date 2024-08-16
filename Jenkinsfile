def performRelease = false
def baseNexusUrl = "http://nexus3:8081"
def localNexus = "${baseNexusUrl}/repository/public"
def gradleOpts = "-s --build-cache -PlocalNexus=${localNexus}"
def buildCacheDir = ""
def gradleCmd = './gradlew'

pipeline {
  agent { label 'gradle-8.10-jdk17' }

  parameters {
    string(defaultValue: '', description: 'Extra Gradle Options', name: 'extraGradleOpts')
    booleanParam(name: 'majorRelease', defaultValue: false, description: 'Perform a major release')
    booleanParam(name: 'minorRelease', defaultValue: false, description: 'Perform a minor release')
    booleanParam(name: 'patchRelease', defaultValue: false, description: 'Perform a patch release')
    booleanParam(name: 'publish', defaultValue: false, description: 'Publish to nexus')
    string(name: 'baseBuildCacheDir', defaultValue: '/cache', description: 'Base build cache dir')
    string(name: 'buildCacheName', defaultValue: 'default', description: 'Build cache name')

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
        }
      }
    }

    stage('PrepareRelease') {
      when { expression { performRelease } }
      environment {
        GRADLE_USER_HOME = "${buildCacheDir}"
      }
      steps {
        script {
          sh "$gradleCmd prepareRelease $prepareReleaseOptions $gradleOpts"
        }
      }
    }
		
    stage('Build') {
      environment {
        GRADLE_USER_HOME = "${buildCacheDir}"
      }
      steps {
        sh "$gradleCmd clean build $gradleOpts"
      }
    }
    
    stage('Finalize') {
      when { expression { performRelease || params.publish } }
      environment {
        GRADLE_USER_HOME = "${buildCacheDir}"
      }
      steps {
        script {
          
          if (performRelease || params.publish ) {
            def publishParams = '-PpublishUsername=${publishUsername} -PpublishPassword=${publishPassword}'
            publishParams += " -PpublishSnapshotUrl=${baseNexusUrl}/repository/snapshots"
            publishParams += " -PpublishReleaseUrl=${baseNexusUrl}/repository/releases"
            withCredentials([usernamePassword(credentialsId: 'nexus.fincherhome.com', usernameVariable: 'publishUsername', passwordVariable: 'publishPassword')]) {
              sh "${gradleCmd} publish ${publishParams} ${gradleOpts}" 
            }
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

