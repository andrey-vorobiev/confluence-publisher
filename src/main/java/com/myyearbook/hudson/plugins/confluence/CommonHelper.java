package com.myyearbook.hudson.plugins.confluence;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

public class CommonHelper
{
    public static void log(BuildListener listener, String message)
    {
        listener.getLogger().println("[confluence] " + message);
    }

    public static String expand(AbstractBuild build, BuildListener listener, String source)
    {
        try
        {
            return build.getEnvironment(listener).expand(source);
        }
        catch (Exception e)
        {
            e.printStackTrace(listener.getLogger());

            return source;
        }
    }
}
