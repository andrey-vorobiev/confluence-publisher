
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
    public String performEdits(BuildListener listener,String content, String generated, boolean isNewFormat)
    {
        final StringBuilder sb = new StringBuilder(content);

        // Prepend the generated content to the beginning of the page
        if (isNewFormat) {
            // New format: wrap the new content with <p />
            sb.insert(0, generated);
        } else {
            // Insert newline at the beginning, generated content before that
            sb.insert(0, '\n').insert(0, generated);
        }
        return sb.toString();
    }
}
