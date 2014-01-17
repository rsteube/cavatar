package com.cutlass.confluence.plugin.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.atlassian.confluence.setup.quartz.AbstractClusterAwareQuartzJobBean;
import com.cutlass.confluence.plugin.rest.HashTranslator;

public class PopulateMapJob extends AbstractClusterAwareQuartzJobBean
{

    @Override
    protected void executeJob(final JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        final HashTranslator hashTranslator = HashTranslator.getInstance();
        hashTranslator.populateTranslation();
    }

}
