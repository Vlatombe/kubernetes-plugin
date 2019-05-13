podTemplate(label: 'mypod', yaml: """
apiVersion: v1
kind: Pod
metadata:
spec:
  containers:
  - name: jnlp
    volumeMounts:
    - mountPath: /tmp
      name: tmp
  - name: ssh-client
    image: kroniak/ssh-client:3.6
    command:
    - cat
    tty: true
    volumeMounts:
    - mountPath: /tmp
      name: tmp
  volumes:
  - name: tmp
    emptyDir: {}
"""
)
{
    node ('mypod') {
        stage('container log') {
            container('ssh-client') {
                sshagent (credentials: ['ContainerExecDecoratorPipelineTest-sshagent']) {
                    sh 'env'
                    sh 'ssh-add -L'
                    sh 'ssh -vT -o "StrictHostKeyChecking=no" git@github.com || exit 0'
                }
            }
            sshagent (credentials: ['ContainerExecDecoratorPipelineTest-sshagent']) {
                container('ssh-client') {
                    sh 'env'
                    sh 'ssh-add -L'
                    sh 'ssh -vT -o "StrictHostKeyChecking=no" git@github.com || exit 0'
                }
            }
        }
    }
}
