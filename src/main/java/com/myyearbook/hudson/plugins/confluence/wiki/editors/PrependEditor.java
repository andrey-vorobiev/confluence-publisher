
package com.myyearbook.hudson.plugins.confluence.wiki.editors;

import hudson.Extension;
import hudson.model.BuildListener;

import org.kohsuke.stapler.DataBoundConstructor;

import com.myyearbook.hudson.plugins.confluence.wiki.generators.MarkupGenerator;

/**
 * Represents a simple Wiki markup editor that prepends the content to the
 * beginning of the page. This editor requires no replacement tokens.
 *
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public class PrependEditor extends MarkupEditor
{
    @Extension
    public static final class DescriptorImpl extends MarkupEditorDescriptor
    {
        @Override
        public String getDisplayName()
        {
            return "Prepend content";
        }
    }

    @DataBoundConstructor
    public PrependEditor(MarkupGenerator generator)
    {
        super(generator);
    }

    @Override
    public String performEdits(String content, String generated, boolean useNewFormat)
    {
        StringBuilder sb = new StringBuilder(content);

        sb.insert(0, getSeparator(useNewFormat) + generated);

        return sb.toString();
    }
}
