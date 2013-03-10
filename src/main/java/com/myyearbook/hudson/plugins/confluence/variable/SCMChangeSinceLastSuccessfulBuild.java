package com.myyearbook.hudson.plugins.confluence.variable;

public class SCMChangeSinceLastSuccessfulBuild extends SCMChangesSinceLastBuild
{
    @Override
    public String getName()
    {
        return "CHANGES_SINCE_LAST_SUCCESS";
    }
}
