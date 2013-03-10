package com.myyearbook.hudson.plugins.confluence.variable;

import com.myyearbook.hudson.plugins.confluence.BuildWrapper;

public class LastSCMChanges extends SCMChangesSinceLastBuild
{
    @Override
    public String getName()
    {
        return "LAST_SCM_CHANGE";
    }

    @Override
    public String getValue(BuildWrapper wrapper)
    {
        return super.getValue(wrapper);
    }
}
