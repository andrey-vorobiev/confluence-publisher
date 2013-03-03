
package com.myyearbook.hudson.plugins.confluence.wiki.generators;

import com.myyearbook.hudson.plugins.confluence.BuildWrapper;
import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;

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

    public abstract String generateMarkup(BuildWrapper build);
}
