package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.softartisans.timberwolf.MailboxItem;

import java.util.Iterator;
import java.util.Queue;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * This Iterator will request a list of all ids from the exchange service
 * and then get actual mail items for those ids.
 */
public final class EmailIterator implements Iterator<MailboxItem>
{
    private static final Logger LOG = LoggerFactory.getLogger(FindFolderHelper.class);

    private final ExchangeService exchangeService;
    private int maxFindItemsEntries;
    private int maxGetItemsEntries;
    private int currentMailboxItemIndex = 0;
    private int currentIdIndex = 0;
    private int findItemsOffset = 0;
    private Vector<String> currentIds;
    private Vector<MailboxItem> mailboxItems;

    /** A Queue for managing the folder id's encountered during traversal. */
    private Queue<String> folderQueue;
    private String currentFolder;

    EmailIterator(final ExchangeService service, final int maximumFindItemsEntries,
                  final int maximumGetItemsEntries)
    {
        this.exchangeService = service;
        maxFindItemsEntries = maximumFindItemsEntries;
        maxGetItemsEntries = maximumGetItemsEntries;

        try
        {
            folderQueue = FindFolderHelper.findFolders(exchangeService,
                    FindFolderHelper.getFindFoldersRequest(DistinguishedFolderIdNameType.MSGFOLDERROOT));
            if (folderQueue.size() == 0)
            {
                LOG.info("Did not find any folders.");
                mailboxItems = new Vector<MailboxItem>(0);
                currentIds = new Vector<String>(0);
                return;
            }
            currentFolder = folderQueue.poll();
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

        downloadMoreIds();
        downloadMoreMailboxItems();
    }

    private void downloadMoreIds()
    {
        try
        {
            currentMailboxItemIndex = 0;
            currentIdIndex = 0;
            currentIds = FindItemHelper.findItems(exchangeService, currentFolder, findItemsOffset, maxFindItemsEntries);
            findItemsOffset += currentIds.size();
            LOG.debug("Got {} email ids.", currentIds.size());
        }
        catch (ServiceCallException e)
        {
            LOG.error("Failed to find item ids.", e);
            throw new ExchangeRuntimeException("Failed to find item ids.", e);
        }
        catch (HttpErrorException e)
        {
            LOG.error("Failed to find item ids.", e);
            throw new ExchangeRuntimeException("Failed to find item ids.", e);
        }
    }

    private void downloadMoreMailboxItems()
    {
        try
        {
            currentMailboxItemIndex = 0;
            mailboxItems = GetItemHelper.getItems(maxGetItemsEntries, currentIdIndex, currentIds, exchangeService);
            currentIdIndex += mailboxItems.size();
            LOG.debug("Got {} emails.", mailboxItems.size());
        }
        catch (ServiceCallException e)
        {
            LOG.error("Failed to get item details.", e);
            throw new ExchangeRuntimeException("Failed to get item details.", e);
        }
        catch (HttpErrorException e)
        {
            LOG.error("Failed to get item details.", e);
            throw new ExchangeRuntimeException("Failed to get item details.", e);
        }
    }

    /**
     * This will be wrong in one specific case.  If the page size is a factor of
     * the number of emails on the server, then this check will return true
     * (meaning there are more ids) when in fact we've read to the end of the
     * data, we just can't tell.
     */
    private boolean moreIdsOnServer()
    {
        return currentIds.size() == maxFindItemsEntries;
    }

    private boolean moreItemsOnServer()
    {
        return currentIdIndex < currentIds.size();
    }

    private boolean moreItemsLocally()
    {
        return currentMailboxItemIndex < mailboxItems.size();
    }

    private MailboxItem advanceLocally()
    {
        MailboxItem item = mailboxItems.get(currentMailboxItemIndex);
        currentMailboxItemIndex++;
        return item;
    }

    @Override
    public boolean hasNext()
    {
        boolean definitelyHasMoreItems = moreItemsLocally() || moreItemsOnServer();
        if (definitelyHasMoreItems)
        {
            return true;
        }

        if (!moreIdsOnServer())
        {
            if (folderQueue.size() > 0)
            {
                currentFolder = folderQueue.poll();
                findItemsOffset = 0;
                downloadMoreIds();
                downloadMoreMailboxItems();
                return hasNext();
            }
            else
            {
                return false;
            }
        }

        // This really goes with the javadocs for moreIdsOnServer. Basically, we're going to attempt
        // to get the final page. If we really were unlucky then the evaluation will fail. Otherwise, everything
        // can proceed smoothly.

        downloadMoreIds();
        downloadMoreMailboxItems();

        return mailboxItems.size() > 0;
    }

    @Override
    public MailboxItem next()
    {
        if (moreItemsLocally())
        {
            return advanceLocally();
        }
        else if (moreItemsOnServer())
        {
            downloadMoreMailboxItems();
            return advanceLocally();
        }
        else
        {
            LOG.debug("All done, " + currentMailboxItemIndex + " >= " + mailboxItems.size());
            return null;
        }
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
