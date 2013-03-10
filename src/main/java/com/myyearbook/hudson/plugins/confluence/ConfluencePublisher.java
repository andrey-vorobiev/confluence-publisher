package com.myyearbook.hudson.plugins.confluence;

import com.myyearbook.hudson.plugins.confluence.variable.EnvironmentVariablesHolder;
import com.myyearbook.hudson.plugins.confluence.wiki.editors.MarkupEditor;

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

    private transient EnvironmentVariablesHolder variablesHolder = new EnvironmentVariablesHolder();

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

    private Object readResolve()
    {
        variablesHolder = new EnvironmentVariablesHolder();

        return this;
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

    protected BuildWrapper newBuildWrapper(final AbstractBuild build, final BuildListener listener)
    {
        return new BuildWrapper()
        {
            @Override
            public void log(String message)
            {
                listener.getLogger().println("[confluence] " + message);
            }

            @Override
            public AbstractBuild getBuild()
            {
                return build;
            }

            @Override
            public EnvVars getEnvVars()
            {
                try
                {
                    EnvVars envVars = build.getEnvironment(listener);

                    return variablesHolder.update(envVars, this);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public FilePath getWorkspace()
            {
                return build.getWorkspace();
            }

            @Override
            public File getArtifactsDir()
            {
                return build.getArtifactsDir();
            }
        };
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws RemoteException
    {
        try
        {
            ConfluenceSite site = descriptor.findSite(siteName);

            BuildWrapper wrapper = newBuildWrapper(build, listener);

            ConfluenceSession session = site.createSession();

            RemotePageSummary pageSummary = session.getPageSummary(wrapper.expand(spaceName), wrapper.expand(pageName));

            attachFiles(wrapper, session, pageSummary);

            if (!editors.isEmpty())
            {
                if (!session.isVersion4() && pageSummary instanceof RemotePage)
                {
                    performWikiReplacements(wrapper, session, (RemotePage) pageSummary);
                }
                else
                {
                    jenkins.plugins.confluence.soap.v2.RemotePage pageDataV2 = session.getPageV2(pageSummary.getId());

                    performWikiReplacements(wrapper, session, pageDataV2);
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

    private void attachFiles(BuildWrapper build, ConfluenceSession session, RemotePageSummary pageSummary) throws IOException, InterruptedException
    {
        if (build.getWorkspace() != null)
        {
            build.log("Uploading attachments to: " + pageSummary.getUrl());

            List<FilePath> attachments = new ArrayList<FilePath>();

            if (attachArchivedArtifacts)
            {
                attachments.addAll(getFileSubtree(build.getArtifactsDir()));
            }

            if (!isEmpty(fileSet))
            {
                for (FilePath workspaceFile : build.getWorkspace().list(build.expand(fileSet)))
                {
                    if (!attachments.contains(workspaceFile))
                    {
                        attachments.add(workspaceFile);
                    }
                }
            }

            build.log("Uploading " + attachments.size() + " file(s) to Confluence...");

            for (FilePath attachment : attachments)
            {
                try
                {
                    String contentType = defaultString(guessContentTypeFromName(attachment.getName()), "application/octet-stream");

                    build.log("Uploading file: " + attachment.getName() + ", contentType: " + contentType);

                    String attachComment = build.expand("Published from Hudson/Jenkins build: $BUILD_URL");

                    RemoteAttachment result = session.addAttachment(pageSummary.getId(), attachment, contentType, attachComment);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void performWikiReplacements(BuildWrapper build, ConfluenceSession session, jenkins.plugins.confluence.soap.v2.RemotePage page) throws IOException, InterruptedException
    {
        page.setContent(performEdits(build, page.getContent(), true));

        String editComment = build.expand("Published from Hudson/Jenkins build: $BUILD_URL");

        session.updatePageV2(page, new jenkins.plugins.confluence.soap.v2.RemotePageUpdateOptions(false, editComment));
    }

    private void performWikiReplacements(BuildWrapper build, ConfluenceSession session, RemotePage page) throws IOException, InterruptedException
    {
        page.setContent(performEdits(build, page.getContent(), false));

        String editComment = build.expand("Published from Hudson/Jenkins build: $BUILD_URL");

        session.updatePage(page, new RemotePageUpdateOptions(false, editComment));
    }

    private String performEdits(BuildWrapper build, String pageSource, boolean isNewFormat)
    {
        for (MarkupEditor editor : editors)
        {
            build.log("Performing wiki edits: " + editor.getDescriptor().getDisplayName());

            pageSource = editor.performReplacement(build, pageSource, isNewFormat);

        }

        return pageSource;
    }
}