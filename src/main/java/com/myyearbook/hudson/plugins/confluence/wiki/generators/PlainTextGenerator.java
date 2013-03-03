
package com.myyearbook.hudson.plugins.confluence.wiki.generators;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Content generator that takes plain text input from the Job configuration. Any
 * build variables will be replaced.
 *
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public class PlainTextGenerator extends MarkupGenerator
{
    @Extension
    public static class DescriptorImpl extends Descriptor<MarkupGenerator>
    {
        @Override
        public String getDisplayName()
        {
            return "Plain text";
        }
    }

    public final String text;

    @DataBoundConstructor
    public PlainTextGenerator(String text)
    {
        this.text = text;
    }

    @Override
    public String generateMarkup(FilePath workspace, EnvVars vars)
    {
        return vars.expand(text);
    }
}
