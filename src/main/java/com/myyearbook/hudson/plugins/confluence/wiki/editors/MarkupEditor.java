
package com.myyearbook.hudson.plugins.confluence.wiki.editors;

import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Hudson;

import org.kohsuke.stapler.DataBoundConstructor;

import com.myyearbook.hudson.plugins.confluence.wiki.generators.MarkupGenerator;

import java.util.List;

/**
 * Base markup editor class
 *
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public abstract class MarkupEditor implements Describable<MarkupEditor>, ExtensionPoint
{
    /**
     * Markup generator
     */
    public MarkupGenerator generator;

    /**
     * Creates a generic markup editor
     *
     * @param generator Markup generator
     */
    @DataBoundConstructor
    public MarkupEditor(final MarkupGenerator generator)
    {
        this.generator = generator;
    }

    /**
     * Perform modifications to the page originalPage. Default implementation makes
     * no modifications.
     */
    public String performReplacement(FilePath workspace, EnvVars vars, String originalPage, boolean isNewFormat) throws TokenNotFoundException
    {
        String generated = generator.generateMarkup(workspace, vars);

        return performEdits(originalPage, generated, isNewFormat);
    }

    /**
     * Stapler seems to wrap {..} values in double quotes, which breaks marker
     * token searching. This will strip the double quotes from those strings.
     *
     * @return token with wrapping double quotes stripped
     */
    protected final String unquoteToken(String token)
    {
        if (token == null)
        {
            return null;
        }

        if (token.startsWith("\"{") && token.endsWith("}\""))
        {
            return token.substring(1, token.length() - 1);
        }

        return token;
    }

    protected String getSeparator(boolean useNewFormat)
    {
        return useNewFormat ? "" : "\n";
    }

    /**
     * Modify the page markup with the given generated content.
     *
     * @throws TokenNotFoundException
     */
    protected abstract String performEdits(String content, String generated, boolean useNewFormat) throws TokenNotFoundException;

    /**
     * Returns the descriptor for this class
     *
     * @return Descriptor
     */
    @SuppressWarnings("unchecked")
    public Descriptor<MarkupEditor> getDescriptor()
    {
        return Hudson.getInstance().getDescriptor(getClass());
    }

    /**
     * Returns list descriptors for all MarkupEditor implementations.
     *
     * @return List of descriptors
     */
    public static DescriptorExtensionList<MarkupEditor, Descriptor<MarkupEditor>> all()
    {
        return Hudson.getInstance().getDescriptorList(MarkupEditor.class);
    }

    /**
     * Descriptor for markup generators
     *
     * @author Joe Hansche <jhansche@myyearbook.com>
     */
    public static abstract class MarkupEditorDescriptor extends Descriptor<MarkupEditor>
    {
        /**
         * Returns all available MarkupGenerator implementations
         *
         * @return List of MakrupGenerator Descriptors
         */
        public final List<Descriptor<MarkupGenerator>> getGenerators()
        {
            return MarkupGenerator.all();
        }
    }

    /**
     * Exception thrown when the configured token cannot be found in the wiki
     * markup.
     *
     * @author Joe Hansche <jhansche@myyearbook.com>
     */
    public static class TokenNotFoundException extends Exception
    {
        private static final long serialVersionUID = -5759944314599051961L;

        public TokenNotFoundException(String message)
        {
            super(message);
        }
    }
}
