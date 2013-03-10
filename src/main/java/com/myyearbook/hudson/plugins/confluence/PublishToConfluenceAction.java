package com.myyearbook.hudson.plugins.confluence;

import hudson.model.Action;

public class PublishToConfluenceAction implements Action
{
    public String getIconFileName()
    {
        return null;
    }

    public String getDisplayName()
    {
        return "Publish to Confluence";
    }

    public String getUrlName()
    {
        return null;
    }
}
