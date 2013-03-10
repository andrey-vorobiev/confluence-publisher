package com.myyearbook.hudson.plugins.confluence.variable;

import com.myyearbook.hudson.plugins.confluence.BuildWrapper;

public interface Variable
{
    String getName();

    String getValue(BuildWrapper wrapper);
}
