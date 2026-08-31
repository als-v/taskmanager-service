pipeline {
  agent any

  parameters {
    booleanParam(name: 'RUN_DEPLOY', defaultValue: true, description: 'Build and recreate only the API service.')
    booleanParam(name: 'RUN_SEED', defaultValue: false, description: 'Apply demo seed to the already migrated database.')
    booleanParam(name: 'RUN_RESET', defaultValue: false, description: 'Reset Docker Compose containers and named volumes. Must run isolated.')
  }

  stages {
    stage('Validate Parameters') {
      steps {
        script {
          if (params.RUN_RESET && (params.RUN_DEPLOY || params.RUN_SEED)) {
            error('RUN_RESET must be executed isolated. Disable RUN_DEPLOY and RUN_SEED for reset jobs.')
          }
          if (!params.RUN_DEPLOY && !params.RUN_SEED && !params.RUN_RESET) {
            error('Select at least one action: RUN_DEPLOY, RUN_SEED or RUN_RESET.')
          }
        }
      }
    }

    stage('Build and Test') {
      when {
        expression { return params.RUN_DEPLOY }
      }
      steps {
        dir('/workspace/taskmanager-service') {
          sh './mvnw test'
        }
      }
    }

    stage('Deploy') {
      when {
        expression { return params.RUN_DEPLOY }
      }
      steps {
        dir('/workspace/taskmanager-service') {
          sh './scripts/deploy.sh'
        }
      }
    }

    stage('Seed') {
      when {
        expression { return params.RUN_SEED }
      }
      steps {
        dir('/workspace/taskmanager-service') {
          sh './scripts/seed-db.sh'
        }
      }
    }

    stage('Reset') {
      when {
        expression { return params.RUN_RESET }
      }
      steps {
        dir('/workspace/taskmanager-service') {
          sh 'CONFIRM_RESET=true ./scripts/reset-env.sh'
        }
      }
    }
  }
}
