package com.myyearbook.hudson.plugins.confluence;

import com.myyearbook.hudson.plugins.confluence.wiki.editors.MarkupEditor;
import com.myyearbook.hudson.plugins.confluence.wiki.editors.MarkupEditor.TokenNotFoundException;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Saveable;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.util.DescribableList;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import jenkins.plugins.confluence.soap.v1.*;

import static com.myyearbook.hudson.plugins.confluence.CommonHelper.*;
import static java.net.URLConnection.guessContentTypeFromName;
import static org.apache.commons.lang.StringUtils.*;

@SuppressWarnings("unused")
public class ConfluencePublisher extends Notifier implements Saveable
{
    @Extension
    public static final ConfluenceBuildStepDescriptor descriptor = new ConfluenceBuildStepDescriptor();

    private String siteName;

    private String pageName;

    private String spaceName;

    private String fileSet;

    private boolean attachArchivedArtifacts;

    private boolean publishIfBuildFailed;

    private DescribableList<MarkupEditor, Descriptor<MarkupEditor>> editors = new DescribableList<MarkupEditor, Descriptor<MarkupEditor>>(this);

    @DataBoundConstructor
    public ConfluencePublisher(String confluenceSiteName, boolean buildIfUnstable, String confluenceSpaceName, final String confluencePageName, boolean attachArchivedArtifacts, String fileSet, List<MarkupEditor> editorList) throws IOException
    {
        this.siteName = confluenceSiteName;
        this.spaceName = confluenceSpaceName;
        this.pageName = confluencePageName;
        this.publishIfBuildFailed = buildIfUnstable;
        this.attachArchivedArtifacts = attachArchivedArtifacts;
        this.fileSet = fileSet;
        this.editors.addAll(editorList);
    }

    public String getSiteName()
    {
        return siteName;
    }

    public String getPageName()
    {
        return pageName;
    }

    public String getSpaceName()
    {
        return spaceName;
    }

    public String getFileSet()
    {
        return fileSet;
    }

    public boolean shouldAttachArchivedArtifacts()
    {
        return attachArchivedArtifacts;
    }

    public boolean shouldBuildIfUnstable()
    {
        return publishIfBuildFailed;
    }

    private String getEditComment(EnvVars vars)
    {
        return vars.expand("Published from Hudson/Jenkins build: $BUILD_URL");
    }

    private String getAttachComment(EnvVars vars)
    {
        return vars.expand("Published from Hudson/Jenkins build: $BUILD_URL");
    }

    @Exported
    public List<MarkupEditor> getConfiguredEditors()
    {
        return editors.toList();
    }

    @Override
    public BuildStepDescriptor getDescriptor()
    {
        return descriptor;
    }

    public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.BUILD;
    }

    public void save() throws IOException
    {
    }

    private List<FilePath> getFileSubtree(File dir)
    {
        List<FilePath> artifacts = new ArrayList<FilePath>();

        if (dir == null || dir.listFiles() == null)
        {
            return artifacts;
        }

        for (File child : dir.listFiles())
        {
            if (child.isDirectory())
            {
                artifacts.addAll(getFileSubtree(child));
            }
            else
            {
                artifacts.add(new FilePath(child));
            }
        }

        return artifacts;
    }

    @Override
    public boolean needsToRun(Result result)
    {
        return Result.SUCCESS.equals(result) || publishIfBuildFailed;
    }

    protected EnvVars getEnvVars(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException
    {
        EnvVars vars = build.getEnvironment(listener);

        vars.put("BUILD_URL", build.getUrl());
        vars.put("BUILD_RESULT", build.getResult().toString());

        return vars;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws RemoteException
    {
        try
        {
            ConfluenceSite site = descriptor.findSite(siteName);

            EnvVars envVars = getEnvVars(build, listener);

            ConfluenceSession session = site.createSession();

            RemotePageSummary pageSummary = session.getPageSummary(expand(build, listener, spaceName), expand(build, listener, pageName));

            attachFiles(build, envVars, session, pageSummary);

            if (!editors.isEmpty())
            {
                if (!session.isVersion4() && pageSummary instanceof RemotePage)
                {
                    performWikiReplacements(build.getWorkspace(), envVars, session, (RemotePage) pageSummary);
                }
                else
                {
                    jenkins.plugins.confluence.soap.v2.RemotePage pageDataV2 = session.getPageV2(pageSummary.getId());

                    performWikiReplacements(build.getWorkspace(), envVars, session, pageDataV2);
                }
            }

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace(listener.getLogger());

            return false;
        }
    }

    private void attachFiles(AbstractBuild build, EnvVars vars, ConfluenceSession session, RemotePageSummary pageSummary) throws IOException, InterruptedException
    {
        if (build.getWorkspace() != null)
        {
            List<FilePath> attachments = new ArrayList<FilePath>();

            if (attachArchivedArtifacts)
            {
                attachments.addAll(getFileSubtree(build.getArtifactsDir()));
            }

            if (!isEmpty(fileSet))
            {
                for (FilePath workspaceFile : build.getWorkspace().list(vars.expand(fileSet)))
                {
                    if (!attachments.contains(workspaceFile))
                    {
                        attachments.add(workspaceFile);
                    }
                }
            }

            for (FilePath attachment : attachments)
            {
                try
                {
                    String contentType = defaultString(guessContentTypeFromName(attachment.getName()), "application/octet-stream");

                    RemoteAttachment result = session.addAttachment(pageSummary.getId(), attachment, contentType, getAttachComment(vars));
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void performWikiReplacements(FilePath workspace, EnvVars vars, ConfluenceSession session, jenkins.plugins.confluence.soap.v2.RemotePage page) throws IOException, InterruptedException
    {
        page.setContent(performEdits(workspace, vars, page.getContent(), true));

        session.updatePageV2(page, new jenkins.plugins.confluence.soap.v2.RemotePageUpdateOptions(false, getEditComment(vars)));
    }

    private void performWikiReplacements(FilePath workspace, EnvVars vars, ConfluenceSession session, RemotePage page) throws IOException, InterruptedException
    {
        page.setContent(performEdits(workspace, vars, page.getContent(), false));

        session.updatePage(page, new RemotePageUpdateOptions(false, getEditComment(vars)));
    }

    private String performEdits(FilePath workspace, EnvVars vars, String pageSource, boolean isNewFormat)
    {
        for (MarkupEditor editor : editors)
        {
            try
            {
                pageSource = editor.performReplacement(workspace, vars, pageSource, isNewFormat);
            }
            catch (TokenNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        }

        return pageSource;
    }
}