import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

Jenkins instance = Jenkins.get()

def configurePipelineJob = { String jobName,
                             String repositoryUrl,
                             String branch,
                             String scriptPath,
                             String description ->
    WorkflowJob job = instance.getItem(jobName) as WorkflowJob

    if (job == null) {
        job = instance.createProject(WorkflowJob, jobName)
    }

    job.description = description

    List<UserRemoteConfig> remotes = [new UserRemoteConfig(repositoryUrl, 'origin', null, null)]
    List<BranchSpec> branches = [new BranchSpec(branch)]
    GitSCM scm = new GitSCM(remotes, branches, false, [], null, null, [])
    CpsScmFlowDefinition definition = new CpsScmFlowDefinition(scm, scriptPath)
    definition.lightweight = true

    job.definition = definition
    job.save()

    println "Configured Jenkins pipeline job '${jobName}' from ${repositoryUrl} (${branch}, ${scriptPath})"
}

configurePipelineJob(
    'taskmanager-service',
    'file:///workspace/taskmanager-service',
    '*/master',
    'Jenkinsfile',
    'Pipeline local do Task Manager Service criado automaticamente pelo Jenkins.'
)

configurePipelineJob(
    'taskmanager-frontend',
    'file:///workspace/taskmanager-frontend',
    '*/master',
    'Jenkinsfile',
    'Pipeline local do Task Manager Frontend criado automaticamente pelo Jenkins.'
)

instance.save()
