def performRelease = false
def gradleOpts = """--info 
                    -s 
                    --build-cache
                    -PlocalNexus=https://www.fincherhome.com/nexus/content/groups/public 
                    -PpublishUsername=upload -PpublishPassword=upload 
                    -PpublishSnapshotUrl=https://www.fincherhome.com/nexus/nexus/content/repositories/snapshots 
                    -PpublishReleaseUrl=https://www.fincherhome.com/nexus/nexus/content/repositories/releases"""

properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')), 
disableConcurrentBuilds(), pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '1d']])])

pipeline {
    agent any

    parameters {
        string(defaultValue: '', description: 'Perform a release with the given version', name: 'release')
        string(defaultValue: '', description: 'Extra Gradle Options', name: 'extraGradleOpts')
    }

    tools {
        jdk 'jdk8'
    }

    stages {
        stage('Prepare') {
            steps {
                script {
                   if (!params.release.isEmpty()) {
                       performRelease = true
                   }                           
                   if (!params.extraGradleOpts.isEmpty()) {
                       gradleOpts = gradleOpts + extraGradleOpts
                   }
               }
            }
        }
		
        stage('Build') {
            steps {
                sh './gradlew clean build checkstyleMain ' + gradleOpts
            }
        }

        stage('Release') {
            when { expression { performRelease } }
            steps {
                sh "./gradlew release -Prelease.releaseVersion=${params.release} -Prelease.newVersion=${params.release}-SNAPSHOT " + gradleOpts
            }
        }
		
        stage('Publish') {
            steps {
                sh './gradlew publish ' + gradleOpts
            }
        }

    }
}
