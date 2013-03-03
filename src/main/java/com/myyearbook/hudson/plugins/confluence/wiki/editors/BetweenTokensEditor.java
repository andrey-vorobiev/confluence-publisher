
package com.myyearbook.hudson.plugins.confluence.wiki.editors;

import hudson.Extension;
import hudson.Util;
import hudson.model.BuildListener;

import org.kohsuke.stapler.DataBoundConstructor;

import com.myyearbook.hudson.plugins.confluence.wiki.generators.MarkupGenerator;

/**
 * Represents a token-based Wiki markup editor that inserts the new content
 * between two (start/end) replacement marker tokens.
 *
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public class BetweenTokensEditor extends MarkupEditor
{
    @Extension
    public static final class DescriptorImpl extends MarkupEditorDescriptor
    {
        @Override
        public String getDisplayName()
        {
            return "Replace content between start/end tokens";
        }
    }

    public String startMarkerToken;

    public String endMarkerToken;

    @DataBoundConstructor
    public BetweenTokensEditor(MarkupGenerator generator, String startMarkerToken, String endMarkerToken)
    {
        super(generator);

        this.startMarkerToken = unquoteToken(Util.fixEmptyAndTrim(startMarkerToken));
        this.endMarkerToken = unquoteToken(Util.fixEmptyAndTrim(endMarkerToken));
    }

    /**
     * Inserts the generated content in the section between the
     * {@link #startMarkerToken} and {@link #endMarkerToken}.
     *
     */
    @Override
    public String performEdits(String originalPage, String generated, boolean useNewFormat)
    {
        StringBuilder sb = new StringBuilder(originalPage);

        int startMarkerIndex = originalPage.indexOf(startMarkerToken) + startMarkerToken.length(), endMarkerIndex = originalPage.indexOf(endMarkerToken);

        if (startMarkerIndex < 0)
        {
            throw new IllegalArgumentException("Start-marker token could not be found in the page content: "+ startMarkerToken);
        }

        if (endMarkerIndex < 0)
        {
            throw new IllegalArgumentException("End-marker token could not be found in the page content: " + endMarkerToken);
        }

        sb.replace(startMarkerIndex, endMarkerIndex, getSeparator(useNewFormat) + generated + getSeparator(useNewFormat));

        return sb.toString();
    }
}
