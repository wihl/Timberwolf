package com.softartisans.timberwolf.exchange;

import com.softartisans.timberwolf.MailboxItem;
import java.util.Iterator;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An iterator that calls getItem to get a list of items
 */
public class GetItemIterator extends BaseChainIterator<MailboxItem>
{

    private static final Logger LOG = LoggerFactory.getLogger(GetItemIterator.class);
    private ExchangeService service;
    private Vector<String> ids;
    private int currentStart;
    private int pageSize;

    public GetItemIterator(ExchangeService exchangeService, Vector<String> messageIds, int itemsPageSize)
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
            LOG.error("Failed to find item ids.", e);
            throw new ExchangeRuntimeException("Failed to find item ids.", e);
        }
        catch (HttpErrorException e)
        {
            LOG.error("Failed to find item ids.", e);
            throw new ExchangeRuntimeException("Failed to find item ids.", e);
        }
    }
}
