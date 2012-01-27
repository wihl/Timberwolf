package com.ripariandata.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.ripariandata.timberwolf.MailboxItem;
import java.util.Iterator;
import java.util.Queue;
import org.joda.time.DateTime;
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
    private String user;
    private DateTime accessTime;

    public FindFolderIterator(final ExchangeService exchangeService, final Configuration configuration,
                              final String targetUser)
    {
        service = exchangeService;
        config = configuration;
        user = targetUser;

        try
        {
            folderQueue = FindFolderHelper.findFolders(exchangeService,
                                                       FindFolderHelper.getFindFoldersRequest(
                                                               DistinguishedFolderIdNameType.MSGFOLDERROOT), user);
            if (folderQueue.size() == 0)
            {
                LOG.warn("Did not find any folders.");
            }
        }
        catch (ServiceCallException e)
        {
            throw new ExchangeRuntimeException(
                String.format("Failed to find folder ids for %s: %s",
                              user, e.getMessage()),
                e);
        }
        catch (HttpErrorException e)
        {
            throw new ExchangeRuntimeException(
                String.format("Failed to find folder ids for %s: %s",
                              user, e.getMessage()),
                e);
        }

        accessTime = new DateTime();
    }

    @Override
    protected Iterator<MailboxItem> createIterator()
    {
        if (folderQueue.size() == 0)
        {
            config.setLastUpdated(user, accessTime);
            return null;
        }
        FolderContext folder = new FolderContext(folderQueue.poll(), user);
        return new FindItemIterator(service, config, folder);
    }
}
