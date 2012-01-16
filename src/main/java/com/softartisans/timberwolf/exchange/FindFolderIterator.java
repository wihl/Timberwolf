package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.softartisans.timberwolf.MailboxItem;
import java.util.Iterator;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a FindItemIterator for each folder found with findFolders.
 */
public class FindFolderIterator extends BaseChainIterator<MailboxItem>
{
    private static final Logger LOG = LoggerFactory.getLogger(FindFolderIterator.class);
    private ExchangeService service;
    private Configuration config;
    private Queue<String> folderQueue;

    public FindFolderIterator(final ExchangeService exchangeService, final Configuration configuration)
    {
        service = exchangeService;
        config = configuration;

        try
        {
            folderQueue = FindFolderHelper.findFolders(exchangeService,
                                                       FindFolderHelper.getFindFoldersRequest(
                                                               DistinguishedFolderIdNameType.MSGFOLDERROOT));
            if (folderQueue.size() == 0)
            {
                LOG.debug("Did not find any folders.");
            }
        }
        catch (ServiceCallException e)
        {
            LOG.error("Failed to find folder ids.", e);
            throw new ExchangeRuntimeException("Failed to find folder ids.", e);
        }
        catch (HttpErrorException e)
        {
            LOG.error("Failed to find folder ids.", e);
            throw new ExchangeRuntimeException("Failed to find folder ids.", e);
        }
    }

    @Override
    protected Iterator<MailboxItem> createIterator()
    {
        if (folderQueue.size() == 0)
        {
            return null;
        }
        FolderContext folder = new FolderContext(folderQueue.poll());
        return new FindItemIterator(service, config, folder);
    }
}
