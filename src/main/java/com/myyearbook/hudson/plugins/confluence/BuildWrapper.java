package com.myyearbook.hudson.plugins.confluence;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;

import java.io.File;

public abstract class BuildWrapper
{
    public abstract void log(String message);

    public abstract EnvVars getEnvVars();

    public abstract FilePath getWorkspace();

    public abstract File getArtifactsDir();

    public abstract AbstractBuild getBuild();

    public String expand(String source)
    {
        return getEnvVars().expand(source);
    }
}
