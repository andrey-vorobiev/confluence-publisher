package com.myyearbook.hudson.plugins.confluence;

import com.myyearbook.hudson.plugins.confluence.wiki.editors.MarkupEditor;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.plugins.confluence.soap.v1.RemotePageSummary;
import jenkins.plugins.confluence.soap.v1.RemoteSpace;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static hudson.FilePath.validateFileMask;
import static hudson.util.FormValidation.*;
import static org.apache.commons.lang.StringUtils.*;

@SuppressWarnings("unused")
public class ConfluenceBuildStepDescriptor extends BuildStepDescriptor<Publisher>
{
    private List<ConfluenceSite> sites = new ArrayList<ConfluenceSite>();

    public ConfluenceBuildStepDescriptor()
    {
        super(ConfluencePublisher.class);

        load();
    }

    private static String decode(String source)
    {
        try
        {
            return URLDecoder.decode(source, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public List<ConfluenceSite> getSites()
    {
        return sites;
    }

    public ConfluenceSite findSite(String name)
    {
        for (ConfluenceSite site : sites)
        {
            if (site.getName().equals(name))
            {
                return site;
            }
        }

        throw new IllegalArgumentException("Site not found: " + name);
    }

    public List<Descriptor<MarkupEditor>> getEditors()
    {
        return MarkupEditor.all();
    }

    @Override
    public boolean isApplicable(Class jobType)
    {
        return !sites.isEmpty();
    }

    @Override
    public String getDisplayName()
    {
        return "Publish to Confluence";
    }

    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException
    {
        return req.bindJSON(ConfluencePublisher.class, formData);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException
    {
        sites = req.bindJSONToList(ConfluenceSite.class, json.get("sites"));

        save();

        return true;
    }

    public FormValidation doSpaceNameCheck(@QueryParameter String siteName, @QueryParameter String spaceName)
    {
        siteName = decode(siteName); spaceName = decode(spaceName);

        ConfluenceSite site = findSite(siteName);

        if (site == null)
        {
            return error("Unknown site:" + siteName);
        }

        if (isEmpty(spaceName))
        {
            return ok();
        }

        try
        {
            RemoteSpace space = site.createSession().getSpace(spaceName);

            return space != null ? ok("OK: " + space.getName()) : error("Space not found");
        }
        catch (RemoteException e)
        {
            return contains(spaceName, '$') ? warning("Space not found (ignoring build-time parameter)") : error(e, "Space not found");
        }
    }

    public FormValidation doPageNameCheck(@QueryParameter String siteName, @QueryParameter String spaceName, @QueryParameter String pageName)
    {
        siteName = decode(siteName); spaceName = decode(spaceName); pageName = decode(pageName);

        ConfluenceSite site = findSite(siteName);

        if (site == null)
        {
            return error("Unknown site: " + siteName);
        }

        if (isEmpty(spaceName) || isEmpty(pageName))
        {
            return ok();
        }

        try
        {
            RemotePageSummary page = site.createSession().getPageSummary(spaceName, pageName);

            return page == null ? error("Page not found") : ok("OK: " + page.getTitle());
        }
        catch (RemoteException e)
        {
            if (contains(pageName, '$') || contains(spaceName, '$'))
            {
                return warning("Page not found (ignoring build-time parameter)");
            }

            return error(e, "Page not found");
        }
    }

    public FormValidation doMaskCheck(@AncestorInPath AbstractProject project, @QueryParameter String mask) throws IOException, InterruptedException
    {
        return validateFileMask(project.getSomeWorkspace(), decode(mask));
    }
}
