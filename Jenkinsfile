pipeline {
  agent { label 'docker' }

  options {
      disableConcurrentBuilds()
  }
  
  environment {
      DEPLOY_JOB = "gucci-demo-kafka-deploy"
      ARTIFACT_ID = "gucci-demo-kafka"
      ARTIFACT_BUILDER_ID = "gucci-demo-kafka-builder"
      DEV_JENKINS = credentials('dev-jenkins')
      DOCKER_REGISTRY = "dockerdev.repos.regiongold.com"
      NEXUS_CREDENTIALS = credentials('nexus-oms')
  }

  stages {

    stage('Update Docker Image Builder') {
      when {
        expression { "${params.ENVIRONMENT}" == 'qa' || "${params.ENVIRONMENT}" == 'pre' }
      }

      steps {
        sh("docker login -u ${DEV_JENKINS_USR} -p ${DEV_JENKINS_PSW} https://${DOCKER_REGISTRY}")
        sh("chmod +x create_builder_image.sh")
        sh("./create_builder_image.sh ${DOCKER_REGISTRY}/${ARTIFACT_BUILDER_ID} ${params.ENVIRONMENT}")
      }
    }

    stage('Build Shadow jar') {
      when {
        expression { "${params.ENVIRONMENT}" == 'qa' || "${params.ENVIRONMENT}" == 'pre' }
      }

      steps {
        sh("mkdir -p build")
        sh("chmod +x create_shadowjar.sh")
        sh("./create_shadowjar.sh ${DOCKER_REGISTRY}/${ARTIFACT_BUILDER_ID} ${params.ENVIRONMENT}")
      }
    }

    stage('Build Docker Service Image') {
      when {
        expression { "${params.ENVIRONMENT}" == 'qa' || "${params.ENVIRONMENT}" == 'pre' }
      }

      steps {
        sh("docker login -u ${DEV_JENKINS_USR} -p ${DEV_JENKINS_PSW} https://${DOCKER_REGISTRY}")
        sh("chmod +x create_service_image.sh")
        sh("./create_service_image.sh ${DOCKER_REGISTRY}/${ARTIFACT_ID}")
      }
    }

    stage('Sync Docker images') {
      when {
            expression { "${params.ENVIRONMENT}" == 'qa' || "${params.ENVIRONMENT}" == 'pre' }
      }

      steps {
        sh("docker login -u ${DEV_JENKINS_USR} -p ${DEV_JENKINS_PSW} https://${DOCKER_REGISTRY}")
        sh("chmod +x sync_images.sh")
        sh("./sync_images.sh ${DOCKER_REGISTRY}/${ARTIFACT_ID} ${DOCKER_REGISTRY}/${ARTIFACT_BUILDER_ID} ${params.ENVIRONMENT}")
        echo 'Builder and Service images have been pushed to registries'
      }
    }

    stage('Redeploy ECS instance in QA') {
      when {
        expression { "${params.ENVIRONMENT}" == 'qa' }
      }

      steps {
        sh("aws --region 'us-east-1' ecs update-service --cluster 'team-ecs01' --service 'team-qa-krwmsbridge' --force-new-deployment")
      }
    }

    stage('Redeploy ECS instance in PREPROD') {
      when {
        expression { "${params.ENVIRONMENT}" == 'pre' }
      }

      steps {
        sh("aws --region 'us-east-1' ecs update-service --cluster 'team-ecs01' --service 'team-pre-krwmsbridge' --force-new-deployment")
      }
    }
  }

  post {
    success {
      echo 'Process completed'
    }

    failure {
      echo 'Process completed with failures'
    }
  }
}