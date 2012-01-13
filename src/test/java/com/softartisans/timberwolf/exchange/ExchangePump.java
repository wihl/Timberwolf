package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.CreateFolderResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.CreateFolderType;
import com.microsoft.schemas.exchange.services.x2006.messages.FolderInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfFoldersType;
import com.microsoft.schemas.exchange.services.x2006.types.TargetFolderIdType;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import org.xmlsoap.schemas.soap.envelope.EnvelopeDocument;
import org.xmlsoap.schemas.soap.envelope.EnvelopeType;

/**
 * This class puts a bunch of folders or emails into an account in exchange.
 * This should only be used by integration tests.
 */
public class ExchangePump
{
    private static final String DECLARATION = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    private static final String SOAP_ENCODING = "UTF-8";

    private String endpoint;
    private HttpUrlConnectionFactory connectionFactory = new AlfredoHttpUrlConnectionFactory();

    public ExchangePump(String exchangeUrl)
    {
        endpoint = exchangeUrl;
    }

    private List<String> createFolders(TargetFolderIdType parentFolderId, String... folderNames)
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        CreateFolderType createFolder = envelope.addNewBody().addNewCreateFolder();
        createFolder.setParentFolderId(parentFolderId);
        NonEmptyArrayOfFoldersType requestedFolders = createFolder.addNewFolders();
        for (String folderName : folderNames)
        {
            requestedFolders.addNewFolder().setDisplayName(folderName);
        }
        CreateFolderResponseType response = sendRequest(request).getCreateFolderResponse();
        FolderInfoResponseMessageType[] array = response.getResponseMessages().getCreateFolderResponseMessageArray();
        List<String> folderIds = new ArrayList<String>();
        for (FolderInfoResponseMessageType folderResponse : array)
        {
            for (FolderType folder : folderResponse.getFolders().getFolderArray())
            {
                folderIds.add(folder.getFolderId().getId());
            }
        }
        return folderIds;
    }

    public List<String> createFolders(String parent, String... folderNames)
    {
        TargetFolderIdType parentFolder = TargetFolderIdType.Factory.newInstance();
        parentFolder.addNewFolderId().setId(parent);
        return createFolders(parentFolder, folderNames);
    }

    public List<String> createFolders(DistinguishedFolderIdNameType.Enum parent, String... folderNames)
    {
        TargetFolderIdType parentFolder = TargetFolderIdType.Factory.newInstance();
        parentFolder.addNewDistinguishedFolderId().setId(parent);
        return createFolders(parentFolder, folderNames);
    }

    public List<String> createMessages(String folderId, Message... messages)
    {
        return null;
    }

    public List<String> createMessages(DistinguishedFolderIdNameType.Enum folderId, Message... messages)
    {
        return null;
    }

    private org.xmlsoap.schemas.soap.envelope.BodyType sendRequest(final EnvelopeDocument envelope)
    {
        String request = DECLARATION + envelope.xmlText();
        try
        {
            HttpURLConnection conn = connectionFactory.newInstance(endpoint, request.getBytes(SOAP_ENCODING));

            int code = conn.getResponseCode();

            InputStream responseData = conn.getInputStream();

            int amtAvailable = responseData.available();

            if (code == HttpURLConnection.HTTP_OK)
            {
                EnvelopeDocument response = EnvelopeDocument.Factory.parse(responseData);
                return response.getEnvelope().getBody();
            }
            else
            {
                System.err.println("HTTP_ERROR: " + code);
                return null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

    }

    /** A simple class representing an email */
    public class Message
    {

    }
}
