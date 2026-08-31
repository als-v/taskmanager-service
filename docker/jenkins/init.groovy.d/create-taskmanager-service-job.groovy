import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

String jobName = 'taskmanager-service'
String repositoryUrl = 'file:///workspace/taskmanager-service'
String branch = '*/master'
String scriptPath = 'Jenkinsfile'

Jenkins instance = Jenkins.get()
WorkflowJob job = instance.getItem(jobName) as WorkflowJob

if (job == null) {
  job = instance.createProject(WorkflowJob, jobName)
  job.description = 'Pipeline local do Task Manager Service criado automaticamente pelo Jenkins.'
}

List<UserRemoteConfig> remotes = [new UserRemoteConfig(repositoryUrl, 'origin', null, null)]
List<BranchSpec> branches = [new BranchSpec(branch)]
GitSCM scm = new GitSCM(remotes, branches, false, [], null, null, [])
CpsScmFlowDefinition definition = new CpsScmFlowDefinition(scm, scriptPath)
definition.lightweight = true

job.definition = definition
job.save()
instance.save()
println "Configured Jenkins pipeline job '${jobName}' from ${repositoryUrl} (${branch}, ${scriptPath})"
