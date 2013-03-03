package com.myyearbook.hudson.plugins.confluence;

import com.myyearbook.hudson.plugins.confluence.wiki.editors.MarkupEditor;
import com.myyearbook.hudson.plugins.confluence.wiki.editors.MarkupEditor.TokenNotFoundException;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.plugins.confluence.soap.v1.*;

import static java.net.URLConnection.guessContentTypeFromName;
import static org.apache.commons.lang.StringUtils.*;

@SuppressWarnings("unused")
public class ConfluencePublisher extends Notifier implements Saveable
{
    @Extension
    public static final ConfluenceBuildStepDescriptor descriptor = new ConfluenceBuildStepDescriptor();

    private static final Logger log = Logger.getLogger(ConfluencePublisher.class.getName());

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

    private static void log(BuildListener listener, String message)
    {
        listener.getLogger().println("[confluence] " + message);
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

    private String expand(AbstractBuild build, BuildListener listener, String source)
    {
        try
        {
            return build.getEnvironment(listener).expand(source);
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "Unable to expand source: " + source, e);
            return source;
        }
    }

    private String getEditComment(AbstractBuild build, BuildListener listener)
    {
        return expand(build, listener, "Published from Hudson/Jenkins build: $BUILD_URL");
    }

    private String getAttachComment(AbstractBuild build, BuildListener listener)
    {
        return expand(build, listener, "Published from Hudson/Jenkins build: $BUILD_URL");
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

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws RemoteException
    {
        try
        {
            ConfluenceSite site = descriptor.findSite(siteName);

            build.addAction(new EnvVarAction("BUILD_RESULT", build.getResult().toString()));

            ConfluenceSession session = site.createSession();

            String spaceName = expand(build, listener, this.spaceName), pageName = expand(build, listener, this.pageName);

            RemotePageSummary pageSummary = session.getPageSummary(spaceName, pageName);

            attachFiles(build, listener, session, pageSummary);

            if (!editors.isEmpty())
            {
                if (!session.isVersion4() && pageSummary instanceof RemotePage)
                {
                    performWikiReplacements(build, listener, session, (RemotePage) pageSummary);
                }
                else
                {
                    jenkins.plugins.confluence.soap.v2.RemotePage pageDataV2 = session.getPageV2(pageSummary.getId());

                    performWikiReplacements(build, listener, session, pageDataV2);
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

    private void attachFiles(AbstractBuild build, BuildListener listener, ConfluenceSession session, RemotePageSummary pageSummary) throws IOException, InterruptedException
    {
        if (build.getWorkspace() != null)
        {
            log(listener, "Uploading attachments to: " + pageSummary.getUrl());

            List<FilePath> attachments = new ArrayList<FilePath>();

            if (attachArchivedArtifacts)
            {
                attachments.addAll(getFileSubtree(build.getArtifactsDir()));
            }

            if (!isEmpty(fileSet))
            {
                for (FilePath workspaceFile :build.getWorkspace().list(expand(build, listener, fileSet)))
                {
                    if (!attachments.contains(workspaceFile))
                    {
                        attachments.add(workspaceFile);
                    }
                }
            }

            log(listener, "Uploading " + attachments.size() + " file(s) to Confluence...");

            for (FilePath attachment : attachments)
            {
                try
                {
                    String contentType = defaultString(guessContentTypeFromName(attachment.getName()), "application/octet-stream");

                    log(listener, "Uploading file: " + attachment.getName() + ", contentType: " + contentType);

                    RemoteAttachment result = session.addAttachment(pageSummary.getId(), attachment, contentType, getAttachComment(build, listener));
                }
                catch (Exception e)
                {
                    log(listener, "Upload failed: " + attachment.getName());
                    e.printStackTrace(listener.getLogger());
                }
            }
        }
        else
        {
            log(listener, "Workspace is unavailable");
        }
    }

    private void performWikiReplacements(AbstractBuild build, BuildListener listener, ConfluenceSession confluence, jenkins.plugins.confluence.soap.v2.RemotePage pageDataV2) throws IOException, InterruptedException
    {
        String content = performEdits(build, listener, pageDataV2.getContent(), true);

        pageDataV2.setContent(content);

        confluence.updatePageV2(pageDataV2, new jenkins.plugins.confluence.soap.v2.RemotePageUpdateOptions(false, getEditComment(build, listener)));
    }

    private void performWikiReplacements(AbstractBuild build, BuildListener listener, ConfluenceSession confluence, RemotePage pageData) throws IOException, InterruptedException
    {
        String content = performEdits(build, listener, pageData.getContent(), false);

        pageData.setContent(content);

        confluence.updatePage(pageData, new RemotePageUpdateOptions(false, getEditComment(build, listener)));
    }

    private String performEdits(AbstractBuild build, BuildListener listener, String content, boolean isNewFormat)
    {
        for (MarkupEditor editor : editors)
        {
            log(listener, "Performing wiki edits: " + editor.getDescriptor().getDisplayName());

            try
            {
                content = editor.performReplacement(build, listener, content, isNewFormat);
            }
            catch (TokenNotFoundException e)
            {
                log(listener, "ERROR while performing replacement: " + e.getMessage());
            }
        }

        return content;
    }
}