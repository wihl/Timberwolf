package com.softartisans.timberwolf;

import com.microsoft.schemas.exchange.services._2006.messages.FindItemDocument;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseDocument;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services._2006.types.ItemResponseShapeType;
import com.microsoft.schemas.exchange.services._2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfBaseFolderIdsType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdNameType;

import com.softartisans.timberwolf.exchangeservice.ExchangeServiceStub;

import java.util.Iterator;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;

/**
 * ExchangeMailStore represents a remote Exchange mail store.  It uses the
 * Exchange Web Services API to communicate with the Exchange server.
 */
public class ExchangeMailStore implements MailStore
{
    ExchangeServiceStub stub;
    private String user;
    private String password;

    /**
     * Constructor that takes authentication information and a service endpoint.
     *
     * @param user The name of the user that ExchangeMailStore will authenticate
     * to the server with.
     *
     * @param password The password of the user that ExchangeMailStore will
     * authenticate to the server with.
     *
     * @param endpoint The location of the service endpoint that
     * ExchangeMailStore will connect to.
     */
    public ExchangeMailStore(String user, String password, String endpoint)
    {
        try
        {         
            stub = new ExchangeServiceStub(endpoint);
            this.user = user;
            this.password = password;   
        }        
        catch (AxisFault e)
        {
            System.out.println(e.getMessage());
        }
    }

    public Iterator<MailboxItem> getMail(String user)
    {
        try
        {
            FindItemDocument request = FindItemDocument.Factory.newInstance();
            FindItemType find = request.addNewFindItem();
            ItemResponseShapeType itemShape = find.addNewItemShape();
            itemShape.setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
            NonEmptyArrayOfBaseFolderIdsType folderIds = 
                find.addNewParentFolderIds();
            DistinguishedFolderIdType parentFolder = 
                folderIds.addNewDistinguishedFolderId();
            parentFolder.setId(DistinguishedFolderIdNameType.INBOX);

            FindItemResponseDocument response = stub.findItem(request, null, null, 
                                                            null, null);
            System.out.println(response.getFindItemResponse()
                                       .getResponseMessages()
                                       .getFindItemResponseMessageArray()
                                       .length);            
        }        
        catch (RemoteException e)
        {
            System.out.println(e.getMessage());
        }

        return null;
    }
}
