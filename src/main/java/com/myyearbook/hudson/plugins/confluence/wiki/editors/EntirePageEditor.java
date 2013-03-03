
package com.myyearbook.hudson.plugins.confluence.wiki.editors;

import hudson.Extension;
import hudson.model.BuildListener;

import org.kohsuke.stapler.DataBoundConstructor;

import com.myyearbook.hudson.plugins.confluence.wiki.generators.MarkupGenerator;

/**
 * Represents a simple Wiki markup editor that replaces the entire page content
 * with the newly-generated content. This editor requires no replacement tokens.
 *
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public class EntirePageEditor extends MarkupEditor
{
    @Extension
    public static final class DescriptorImpl extends MarkupEditorDescriptor
    {
        @Override
        public String getDisplayName()
        {
            return "Replace entire page content";
        }
    }

    @DataBoundConstructor
    public EntirePageEditor(MarkupGenerator generator)
    {
        super(generator);
    }

    @Override
    public String performEdits(String content, String generated, boolean useNewFormat)
    {
        return generated;
    }
}