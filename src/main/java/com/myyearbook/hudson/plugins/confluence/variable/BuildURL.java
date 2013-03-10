package com.myyearbook.hudson.plugins.confluence.variable;

import com.myyearbook.hudson.plugins.confluence.BuildWrapper;
import hudson.model.Hudson;

import static hudson.model.Hudson.getInstance;

public class BuildURL implements Variable
{
    public String getName()
    {
        return "BUILD_URL";
    }

    public String getValue(BuildWrapper wrapper)
    {
        return getInstance().getRootUrl() + wrapper.getBuild().getUrl();
    }
}
