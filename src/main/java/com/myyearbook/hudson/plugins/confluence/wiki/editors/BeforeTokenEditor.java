
package com.myyearbook.hudson.plugins.confluence.wiki.editors;

import hudson.Extension;
import hudson.Util;
import hudson.model.BuildListener;

import org.kohsuke.stapler.DataBoundConstructor;

import com.myyearbook.hudson.plugins.confluence.wiki.generators.MarkupGenerator;

/**
 * Represents a token-based Wiki markup editor that inserts the new content
 * immediately before the replacement marker token.
 *
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public class BeforeTokenEditor extends MarkupEditor
{
    @Extension
    public static final class DescriptorImpl extends MarkupEditorDescriptor
    {
        @Override
        public String getDisplayName()
        {
            return "Insert content before token";
        }
    }

    public String markerToken;

    @DataBoundConstructor
    public BeforeTokenEditor(MarkupGenerator generator, String markerToken)
    {
        super(generator);

        this.markerToken = unquoteToken(Util.fixEmptyAndTrim(markerToken));
    }

    @Override
    public String performEdits(String originalPage, String generated, boolean useNewFormat)
    {
        StringBuilder sb = new StringBuilder(originalPage);

        int insertIndex = originalPage.indexOf(markerToken);

        if (insertIndex < 0)
        {
            throw new IllegalArgumentException("Marker token could not be located in the page content: " + markerToken);
        }

        sb.insert(insertIndex, getSeparator(useNewFormat) + generated);

        return sb.toString();
    }
}
