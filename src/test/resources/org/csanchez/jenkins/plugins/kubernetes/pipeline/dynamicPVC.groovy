podTemplate(workspaceVolume: dynamicPVC(requestsSize: "10Gi"), containers: [
        containerTemplate(name: 'jnlp', image: 'jenkins/jnlp-slave:3.35-5-alpine', args: '${computer.jnlpmac} ${computer.name}')
], yaml:'''
spec:
  securityContext:
    fsGroup: 1000
''') {

    node(POD_LABEL) {
        container(name: 'jnlp') {
            sh 'cat /var/run/secrets/kubernetes.io/serviceaccount/namespace'
            git 'https://github.com/jenkinsci/kubernetes-plugin.git'
        }
    }
}
