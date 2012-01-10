package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.softartisans.timberwolf.MailboxItem;
import java.util.Iterator;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iterates over find folder calls
 */
public class FindFolderIterator extends BaseChainIterator<MailboxItem>
{
    private static final Logger LOG = LoggerFactory.getLogger(FindFolderIterator.class);
    private ExchangeService service;
    private int findItemPageSize;
    private int getItemsPageSize;
    private Queue<String> folderQueue;

    public FindFolderIterator(ExchangeService exchangeService, int idsPageSize, int itemsPageSize)
    {
        service = exchangeService;
        findItemPageSize = idsPageSize;
        getItemsPageSize = itemsPageSize;

        try
        {
            folderQueue = FindFolderHelper.findFolders(exchangeService,
                                                       FindFolderHelper.getFindFoldersRequest(DistinguishedFolderIdNameType.MSGFOLDERROOT));
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
        return new FindItemIterator(service,folderQueue.poll(), findItemPageSize, getItemsPageSize);
    }
}
