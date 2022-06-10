def performRelease = false
def gradleOpts = "-s"
gradleOpts += " --build-cache"
gradleOpts += " -PlocalNexus=https://nexus.fincherhome.com/nexus/content/groups/public"
gradleOpts += " -PpublishUsername=upload -PpublishPassword=upload"
gradleOpts += " -PpublishSnapshotUrl=https://nexus.fincherhome.com/nexus/nexus/content/repositories/snapshots"
gradleOpts += " -PpublishReleaseUrl=https://nexus.fincherhome.com/nexus/nexus/content/repositories/releases"

properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')), 
disableConcurrentBuilds(), pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '1d']])])

pipeline {
    agent any

    parameters {
        string(defaultValue: '', description: 'Perform a release with the given version', name: 'release')
        string(defaultValue: '', description: 'Extra Gradle Options', name: 'extraGradleOpts')
    }

    tools {
        jdk 'jdk11'
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
                sh './gradlew clean build ' + gradleOpts
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
