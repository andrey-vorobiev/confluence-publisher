package com.myyearbook.hudson.plugins.confluence.variable;

import com.myyearbook.hudson.plugins.confluence.BuildWrapper;
import hudson.EnvVars;

import java.util.HashSet;
import java.util.Set;

public class EnvironmentVariablesHolder
{
    private Set<Variable> variables = new HashSet<Variable>();

    public EnvironmentVariablesHolder()
    {
        variables.add(new BuildURL());
        variables.add(new LastSCMChanges());
        variables.add(new SCMChangesSinceLastBuild());
        variables.add(new SCMChangeSinceLastSuccessfulBuild());
    }

    public EnvVars update(EnvVars original, BuildWrapper wrapper)
    {
        for (Variable variable : variables)
        {
            original.put(variable.getName(), variable.getValue(wrapper));
        }

        return original;
    }
}
