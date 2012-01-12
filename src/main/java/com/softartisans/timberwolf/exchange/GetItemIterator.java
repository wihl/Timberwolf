package com.softartisans.timberwolf.exchange;

import com.softartisans.timberwolf.MailboxItem;
import java.util.Iterator;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An iterator that calls getItem to get a list of the actual items for
 * the given list of ids.
 *
 * This class pages the calls to getItems.
 */
public class GetItemIterator extends BaseChainIterator<MailboxItem>
{

    private static final Logger LOG = LoggerFactory.getLogger(GetItemIterator.class);
    private ExchangeService service;
    private Vector<String> ids;
    private int currentStart;
    private int pageSize;

    public GetItemIterator(final ExchangeService exchangeService, final Vector<String> messageIds,
                           final int itemsPageSize)
    {
        service = exchangeService;
        ids = messageIds;
        pageSize = itemsPageSize;
        currentStart = 0;
    }

    @Override
    protected Iterator<MailboxItem> createIterator()
    {
        if (currentStart > ids.size())
        {
            return null;
        }
        try
        {
            Vector<MailboxItem> ret = GetItemHelper.getItems(pageSize, currentStart, ids, service);
            LOG.debug("Got {} email ids.", ret.size());
            currentStart += pageSize;
            return ret.iterator();
        }
        catch (ServiceCallException e)
        {
            LOG.error("Failed to get emails.", e);
            throw new ExchangeRuntimeException("Failed to get emails.", e);
        }
        catch (HttpErrorException e)
        {
            LOG.error("Failed to get emails.", e);
            throw new ExchangeRuntimeException("Failed to get emails.", e);
        }
    }
}
