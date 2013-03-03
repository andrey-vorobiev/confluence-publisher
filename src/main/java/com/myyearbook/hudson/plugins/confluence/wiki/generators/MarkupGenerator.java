
package com.myyearbook.hudson.plugins.confluence.wiki.generators;

import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.model.*;

/**
 * Abstract class representing a method of generating Confluence wiki markup.
 *
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public abstract class MarkupGenerator implements Describable<MarkupGenerator>, ExtensionPoint
{
    public MarkupGenerator()
    {
    }

    public static DescriptorExtensionList<MarkupGenerator, Descriptor<MarkupGenerator>> all()
    {
        return Hudson.getInstance().getDescriptorList(MarkupGenerator.class);
    }

    @SuppressWarnings("unchecked")
    public Descriptor<MarkupGenerator> getDescriptor()
    {
        return Hudson.getInstance().getDescriptor(getClass());
    }

    public abstract String generateMarkup(FilePath workspace, EnvVars vars);
}
