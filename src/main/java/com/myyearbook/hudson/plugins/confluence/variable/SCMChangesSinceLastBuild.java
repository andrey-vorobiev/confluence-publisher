package com.myyearbook.hudson.plugins.confluence.variable;

import com.myyearbook.hudson.plugins.confluence.BuildWrapper;
import hudson.scm.ChangeLogSet;

public class SCMChangesSinceLastBuild implements Variable
{
    public String getName()
    {
        return "CHANGES";
    }

    public String getValue(BuildWrapper wrapper)
    {
        StringBuilder sb = new StringBuilder();

        for (ChangeLogSet.Entry change : (ChangeLogSet<ChangeLogSet.Entry>) wrapper.getBuild().getChangeSet())
        {
            sb.append(change.getMsg());
        }

        return sb.toString();
    }
}
