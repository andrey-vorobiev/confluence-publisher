
package com.myyearbook.hudson.plugins.confluence.wiki.generators;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Content generator that reads the markup from a configured workspace file.
 * Build variables will be replaced.
 *
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public class FileGenerator extends MarkupGenerator
{
    @Extension
    public static class DescriptorImpl extends Descriptor<MarkupGenerator>
    {
        @Override
        public String getDisplayName()
        {
            return "File contents";
        }
    }

    @Exported
    public String filename;

    @DataBoundConstructor
    public FileGenerator(String filename)
    {
        this.filename = filename;
    }

    @Override
    public String generateMarkup(FilePath workspace, EnvVars vars)
    {
        if (isEmpty(filename))
        {
            throw new IllegalStateException("No file configured, generating empty markup.");
        }

        try
        {
            FilePath markupFile = workspace.child(filename);

            if (!markupFile.exists())
            {
                throw new IllegalArgumentException("Markup file: " + filename + " doesn't exist.");
            }

            return vars.expand(markupFile.readToString());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
