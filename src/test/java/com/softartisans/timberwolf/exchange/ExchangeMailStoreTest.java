package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseItemIdsType;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Test for ExchangeMailStore, uses mock exchange service
 */
public class ExchangeMailStoreTest
{
    @Before
    public void setUp() throws Exception
    {

    }

    @After
    public void tearDown() throws Exception
    {

    }

    @Test
    public void testGetFindItemsRequestInbox()
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        DistinguishedFolderIdType folderId =
                findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(DistinguishedFolderIdNameType.INBOX);
        assertEquals(findItem.xmlText(),
                     ExchangeMailStore.getFindItemsRequest(
                             DistinguishedFolderIdNameType.INBOX).xmlText());
    }

    @Test
    public void testGetFindItemsRequestDeletedItems()
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        DistinguishedFolderIdType folderId =
                findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(DistinguishedFolderIdNameType.DELETEDITEMS);
        assertEquals(findItem.xmlText(),
                     ExchangeMailStore.getFindItemsRequest(
                             DistinguishedFolderIdNameType.DELETEDITEMS).xmlText());
    }

    @Test
    @Ignore("HAM-33 - I'm not sure what the exchange response here would be "
            + "but I can't get it because ExchangeService doesn't handle it.")
    public void testGetGetItemsRequestNull()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape()
               .setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        assertEquals(getItem.xmlText(),
                     ExchangeMailStore
                             .getGetItemsRequest(null)
                             .xmlText());
    }

    @Test
    @Ignore("HAM-33 - I'm not sure what the exchange response here would be "
            + "but I can't get it because ExchangeService doesn't handle it.")
    public void testGetGetItemsRequest0()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape()
               .setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        assertEquals(getItem.xmlText(),
                     ExchangeMailStore
                             .getGetItemsRequest(new ArrayList<String>())
                             .xmlText());
    }

    @Test
    public void testGetGetItemsRequest1()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape()
               .setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        items.addNewItemId().setId("idNumber0");
        ArrayList<String> ids = new ArrayList<String>();
        ids.add("idNumber0");
        assertEquals(getItem.xmlText(),
                     ExchangeMailStore.getGetItemsRequest(ids).xmlText());
    }

    @Test
    public void testGetGetItemsRequest100()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape()
               .setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        for (int i=0; i<100; i++)
        {
            items.addNewItemId().setId("idNumber" + i);
        }
        ArrayList<String> ids = new ArrayList<String>();
        for (int i=0; i<100; i++)
        {
            ids.add("idNumber" + i);
        }
        assertEquals(getItem.xmlText(),
                     ExchangeMailStore.getGetItemsRequest(ids).xmlText());
    }

}
