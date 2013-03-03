
package com.myyearbook.hudson.plugins.confluence.wiki.editors;

import hudson.Extension;
import hudson.Util;
import hudson.model.BuildListener;

import org.kohsuke.stapler.DataBoundConstructor;

import com.myyearbook.hudson.plugins.confluence.wiki.generators.MarkupGenerator;

/**
 * Represents a token-based Wiki markup editor that inserts the new content
 * immediately following the replacement marker token.
 *
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public class AfterTokenEditor extends MarkupEditor
{
    @Extension
    public static final class DescriptorImpl extends MarkupEditorDescriptor
    {
        @Override
        public String getDisplayName()
        {
            return "Insert content after token";
        }
    }

    public String markerToken;

    @DataBoundConstructor
    public AfterTokenEditor(MarkupGenerator generator, String markerToken)
    {
        super(generator);

        this.markerToken = unquoteToken(Util.fixEmptyAndTrim(markerToken));
    }

    @Override
    public String performEdits(String content, String generated, boolean useNewFormat) throws TokenNotFoundException
    {
        StringBuilder sb = new StringBuilder(content);

        int markerIndex = content.indexOf(markerToken);

        if (markerIndex < 0)
        {
            throw new TokenNotFoundException("Marker token could not be located in the page content: " + markerToken);
        }

        sb.insert(markerIndex + markerToken.length(), getSeparator(useNewFormat) + generated);

        return sb.toString();
    }
}
