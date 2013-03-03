package com.myyearbook.hudson.plugins.confluence;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Action;

/**
 * Build action that is capable of inserting arbitrary KVPs into the EnvVars.
 *
 * @author jhansche
 */
public class EnvVarAction implements Action
{
    private String name;

    private String value;

    public EnvVarAction(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    public String getIconFileName()
    {
        return null;
    }

    public String getDisplayName()
    {
        return null;
    }

    public String getUrlName()
    {
        return null;
    }

    public void buildEnvVars(AbstractBuild build, EnvVars env)
    {
        env.put(name, value);
    }
}
