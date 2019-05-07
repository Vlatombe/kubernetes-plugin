/*
 * Copyright (C) 2017 Original Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.csanchez.jenkins.plugins.kubernetes.pipeline;

import static org.junit.Assert.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.utils.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LoggerRule;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.PrivateKeySource;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class ContainerExecDecoratorPipelineTest extends AbstractKubernetesPipelineTest {

    @Rule
    public LoggerRule containerExecLogs = new LoggerRule()
            .record(Logger.getLogger(ContainerExecDecorator.class.getName()), Level.ALL);


    private void setupSSHCredentials() throws Exception{
        PrivateKeySource source = new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(
                new String(IOUtils.toByteArray(getClass().getResourceAsStream("id_rsa"))));
        BasicSSHUserPrivateKey credentials = new BasicSSHUserPrivateKey(CredentialsScope.GLOBAL,
                "ContainerExecDecoratorPipelineTest-sshagent", "bob", source, "secret_passphrase", "test credentials");
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

    }

    private void assertLogs(WorkflowRun b, String jobIdentifier) throws Exception{
        r.assertLogContains("Identity added:", b);
        //Assert that ssh-agent provided envVar is now properly contributed and set.
        r.assertLogContains("SSH_AGENT_PID=", b);
        //assert that our private key was loaded and is visible within the ssh-agent scope
        r.assertLogContains(String.format("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDhvmTBXRnSbtpnkt/Ldw7ws4LFdoX9oI+5NexgpBC4Otqbn8+Ui6FGWeYflOQUcl3rgmBxsHIeFnPr9qSvgME1TWPIyHSQh2kPMd3NQgkEvioBxghnWRy7sal4KBr2P8m7Iusm8j0aCNLZ3nYjJSywWZxiqqrcpnhFuTD//FPIEhXOu2sk2FEP7YsA9TdL8mAruxy/6Ys2pRC2dQhBtmkEOyEGiBnk3ioT5iCw/Qqe+pU0yaYu69vPyAFCuazBMopPcOuRxFgKvrfCPVqcQb3HERJh5eiW5+5Vg3RwoByQUtQMK5PDBVWPo9srB0Q9Aw9DXmeJCgdtFJqhhh4SR+al /home/jenkins/workspace/%s@tmp/private_key", jobIdentifier),b);
         //check that we don't accidentally start exporting sensitive info  to the log
        r.assertLogNotContains("secret_passphrase", b);
        r.assertLogNotContains("Error connecting to agent: No such file or directory", b);
    }


    @Issue({ "JENKINS-47225", "JENKINS-42582" })
    @Test
    public void sshagent() throws Exception {
        setupSSHCredentials();

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "sshagent");
        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("sshagent.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.waitForCompletion(b);
        assertLogs(b, "sshagent");

    }

    @Test
    public void sshagentFromYaml() throws Exception {

        setupSSHCredentials();
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "sshagentfromyaml");
        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("sshagentfromyaml.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.waitForCompletion(b);
        assertLogs(b, "sshagentfromyaml");
    }

    @Test
    public void sshagentPipeline() throws Exception {

        setupSSHCredentials();
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "sshagentpipeline");
        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("sshagentpipeline.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.waitForCompletion(b);
        assertLogs(b, "sshagentpipeline");
    }

    @Test
    public void sshagentPipelineFromYaml() throws Exception {

        setupSSHCredentials();
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "sshagentpipelinefromyaml");
        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("sshagentpipelinefromyaml.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.waitForCompletion(b);
        assertLogs(b, "sshagentpipelinefromyaml");
    }

    @Test
    public void docker() throws Exception {
        StandardUsernamePasswordCredentials credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
                "ContainerExecDecoratorPipelineTest-docker", "bob", "myusername", "secret_password");
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "docker");
        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("docker.groovy"), true));
        containerExecLogs.capture(1000);
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.waitForCompletion(b);
        // docker login will fail but we can check that it runs the correct command
        r.assertLogContains("Executing command: \"docker\" \"login\" \"-u\" \"myusername\" \"-p\" ******** \"https://index.docker.io/v1/\"", b);
        // check that we don't accidentally start exporting sensitive info to the build log
        r.assertLogNotContains("secret_password", b);
        // check that we don't accidentally start exporting sensitive info to the Jenkins log
        assertFalse("credential leaked to log",
                containerExecLogs.getMessages().stream().anyMatch(msg -> msg.contains("secret_password")));
    }
}
