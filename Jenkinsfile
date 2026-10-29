// ═══════════════════════════════════════════════════════════════════
// Base Pipeline (Jenkinsfile)
//
// Starting point for the workshop. Provisions a Kubernetes
// agent pod with three containers — build-tools (Maven + JDK17),
// oc-tools (OpenShift CLI), and bob (IBM Bob CLI) — sharing a
// workspace-volume emptyDir mounted at /workspace on all three,
// with HOME set to /workspace so Bob can read .bob/custom_modes.yaml
// from the checkout.
//
// This file has a single Checkout stage. Each lab adds one more
// stage beneath it; see labs/LAB<N>_*.md for instructions.
//
// Before running: if your OpenShift project is NOT named `jenkins`,
// update the `bob` container image URL below.
// ═══════════════════════════════════════════════════════════════════
pipeline {
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
spec:
  serviceAccountName: jenkins
  containers:
  - name: build-tools
    image: maven:3.9-eclipse-temurin-17
    command: ['sleep', 'infinity']
    workingDir: /workspace
    volumeMounts:
    - name: workspace-volume
      mountPath: /workspace
    env:
    - name: HOME
      value: /workspace
  - name: oc-tools
    image: quay.io/openshift/origin-cli:latest
    command: ['sleep', 'infinity']
    workingDir: /workspace
    volumeMounts:
    - name: workspace-volume
      mountPath: /workspace
    env:
    - name: HOME
      value: /workspace
  - name: lint-tools
    image: image-registry.openshift-image-registry.svc:5000/jenkins/lint-tools:latest
    command: ['sleep', 'infinity']
    workingDir: /workspace
    volumeMounts:
    - name: workspace-volume
      mountPath: /workspace
    env:
    - name: HOME
      value: /workspace
  - name: bob
    image: image-registry.openshift-image-registry.svc:5000/jenkins/bob-cli:latest
    command: ['sleep', 'infinity']
    workingDir: /workspace
    volumeMounts:
    - name: workspace-volume
      mountPath: /workspace
    env:
    - name: BOBSHELL_API_KEY
      valueFrom:
        secretKeyRef:
          name: bob-cli-credentials
          key: BOBSHELL_API_KEY
    - name: BOB_ACCEPT_LICENSE
      value: "true"
    - name: HOME
      value: /workspace
  volumes:
  - name: workspace-volume
    emptyDir: {}
"""
            defaultContainer 'build-tools'
        }
    }

    stages {
        stage('Checkout') {
            steps {
                echo "=== Checking out repository ==="
                checkout scm

                echo "=== Verifying workspace contents ==="
                sh '''
                    echo "Current directory: $(pwd)"
                    echo "Workspace contents:"
                    ls -la
                '''
            }
        }



        // ── Lab 2: Unit Testing ──────────────────────────────────
        stage('Unit Tests') {
            options {
                timeout(time: 10, unit: 'MINUTES')
            }
            steps {
                script {
                    // Run tests in build-tools (default container); capture exit code
                    // so the pipeline continues even when tests fail
                    def mvnResult = sh(
                        script: 'mvn -f order-service/pom.xml test',
                        returnStatus: true
                    )

                    // Publish JUnit results regardless of mvn exit code
                    junit testResults: 'order-service/target/surefire-reports/*.xml',
                          allowEmptyResults: true

                    if (mvnResult != 0) {
                        echo '════════════════════════════════════════════════════════'
                        echo '  Bob Test Failure Analysis'
                        echo '════════════════════════════════════════════════════════'

                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            def analysis = askBob(
                                """\
Read the Surefire failure reports under order-service/target/surefire-reports/ \
and the relevant source files under order-service/src/. \
Diagnose each failure and suggest a fix.""",
                                'pipeline-test-failure-analyzer'
                            )

                            echo analysis
                            writeFile file: 'bob-test-analysis.md', text: analysis
                        }

                        echo '════════════════════════════════════════════════════════'

                        // Propagate the test failure after Bob has had its say
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'bob-test-analysis.md',
                                     allowEmptyArchive: true
                }
            }
        }

        // ── Lab 3: Security Scanning ─────────────────────────────
        //    Add a scanner stage + Bob CVE/vuln analysis.
        //    See labs/lab3/SECURITY_ANALYSIS.md.

        // ── Lab 4: Linting ───────────────────────────────────────
        //    Add a lint stage + Bob lint analysis + parse and post
        //    Jenkins report as a PR comment.
        //    See labs/lab4/LINTING.md.

    }

    post {
        always {
            echo "=== Pipeline Complete ==="
            echo "Result: ${currentBuild.result ?: 'SUCCESS'}"
        }
    }
}

// ?? Helper: ask Bob, optionally with a specific custom mode ???????????????????
// Writes the prompt to a tempfile in the shared workspace and runs `bob` in
// the bob container.
// Returns the analysis as a string. Using a tempfile (instead of inlining the
// prompt on the command line) avoids shell-escaping issues when the prompt
// contains quotes, backticks, or newlines ? common with diffs.
def askBob(String prompt, String mode = null) {
 container('bob') {
 def promptFile = ".bob-prompt-${System.currentTimeMillis()}.txt"
 writeFile file: promptFile, text: prompt
 def modeFlag = mode ? "--mode ${mode}" : ""
 def analysis = sh(
 script: """cat ${promptFile} | bob run ${modeFlag} --accept-license""",
 returnStdout: true
 ).trim()
 sh "rm -f ${promptFile}"
 return analysis
 }
}