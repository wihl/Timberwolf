package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.CreateFolderResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.CreateFolderType;
import com.microsoft.schemas.exchange.services.x2006.messages.CreateItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.FolderInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.BodyTypeType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemResponseShapeType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageDispositionType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfAllItemsType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfFoldersType;
import com.microsoft.schemas.exchange.services.x2006.types.TargetFolderIdType;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.xmlsoap.schemas.soap.envelope.BodyType;
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
    private String sender;

    public ExchangePump(String exchangeUrl, String senderEmail)
    {
        endpoint = exchangeUrl;
        sender = senderEmail;
    }

    private EnvelopeDocument createEmptyRequest(String user)
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        envelope.addNewHeader().addNewExchangeImpersonation().addNewConnectingSID().setPrincipalName(user);
        return request;
    }

    private List<String> createFolders(String user, TargetFolderIdType parentFolderId, List<RequiredFolder> folders)
    {
        EnvelopeDocument request = createEmptyRequest(user);
        CreateFolderType createFolder = request.getEnvelope().addNewBody().addNewCreateFolder();
        createFolder.setParentFolderId(parentFolderId);
        NonEmptyArrayOfFoldersType requestedFolders = createFolder.addNewFolders();
        for (RequiredFolder folder : folders)
        {
            requestedFolders.addNewFolder().setDisplayName(folder.getName());
        }
        CreateFolderResponseType response = sendRequest(request).getCreateFolderResponse();
        FolderInfoResponseMessageType[] array = response.getResponseMessages().getCreateFolderResponseMessageArray();
        List<String> folderIds = new ArrayList<String>();
        int i=0;
        for (FolderInfoResponseMessageType folderResponse : array)
        {
            for (FolderType folder : folderResponse.getFolders().getFolderArray())
            {
                folders.get(i).setId(folder.getFolderId().getId());
                i++;
            }
        }
        return folderIds;
    }

    public List<String> createFolders(String user, String parent, List<RequiredFolder> folders)
    {
        TargetFolderIdType parentFolder = TargetFolderIdType.Factory.newInstance();
        parentFolder.addNewFolderId().setId(parent);
        return createFolders(user, parentFolder, folders);
    }

    public List<String> createFolders(String user, DistinguishedFolderIdNameType.Enum parent, List<RequiredFolder> folders)
    {
        TargetFolderIdType parentFolder = TargetFolderIdType.Factory.newInstance();
        parentFolder.addNewDistinguishedFolderId().setId(parent);
        return createFolders(user, parentFolder, folders);
    }

    public void sendMessages(List<RequiredEmail> emails) throws FailedToCreateMessage
    {
        EnvelopeDocument request = createEmptyRequest(sender);
        CreateItemType createItem = request.getEnvelope().addNewBody().addNewCreateItem();
        createItem.setMessageDisposition(MessageDispositionType.SEND_ONLY);
        NonEmptyArrayOfAllItemsType items = createItem.addNewItems();

        for (RequiredEmail email : emails)
        {
            MessageType exchangeMessage = items.addNewMessage();
            exchangeMessage.addNewFrom().addNewMailbox().setEmailAddress(sender);
            com.microsoft.schemas.exchange.services.x2006.types.BodyType body = exchangeMessage.addNewBody();
            body.setBodyType(BodyTypeType.TEXT);
            body.setStringValue(email.getBody());
            exchangeMessage.setSubject(email.getSubject());
            exchangeMessage.addNewToRecipients().addNewMailbox().setEmailAddress(email.getTo());
            BodyType response = sendRequest(request);
            ItemInfoResponseMessageType[] responses =
                    response.getCreateItemResponse().getResponseMessages().getCreateItemResponseMessageArray();
            if (responses.length != 1)
            {
                throw new FailedToCreateMessage("There should have been 1 response message to createItem");
            }
            if (responses[0].getResponseCode() != ResponseCodeType.NO_ERROR)
            {
                throw new FailedToCreateMessage("ResponseCode some sort of error: " + responses[0].getResponseCode());
            }
        }
    }

    public HashMap<String, List<MessageId>> findItems(String user) throws FailedToFindMessage
    {
        HashMap<String, List<MessageId>> emailResults = new HashMap<String, List<MessageId>>();
        EnvelopeDocument request = createEmptyRequest(user);
        FindItemType findItem = request.getEnvelope().addNewBody().addNewFindItem();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        ItemResponseShapeType itemShape = findItem.addNewItemShape();
        itemShape.setBaseShape(DefaultShapeNamesType.DEFAULT);
        // I tried to use ID_ONLY and add some AdditionalProperties, but the
        // schema appears to be screwy and not have FieldURI in there correctly
        findItem.addNewParentFolderIds().addNewDistinguishedFolderId().setId(DistinguishedFolderIdNameType.INBOX);
        BodyType response = sendRequest(request);
        FindItemResponseMessageType responseMessage =
                response.getFindItemResponse().getResponseMessages().getFindItemResponseMessageArray()[0];
        if (responseMessage.getResponseCode() != ResponseCodeType.NO_ERROR)
        {
            throw new FailedToFindMessage(
                    "ResponseCode some sort of error: " + responseMessage.getResponseCode());
        }
        for (MessageType email : responseMessage.getRootFolder().getItems().getMessageArray())
        {
            String folderId = RequiredEmail.getFolderId(email.getSubject());
            if (folderId != null)
            {
                List<MessageId> emails = emailResults.get(folderId);
                if (emails == null)
                {
                    emails = new ArrayList<MessageId>();
                    emailResults.put(folderId, emails);
                }
                emails.add(new MessageId(email.getItemId().getId(), email.getItemId().getChangeKey()));
            }
        }
        return emailResults;
    }

    private BodyType sendRequest(final EnvelopeDocument envelope)
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

    public class FailedToCreateMessage extends Exception
    {
        public FailedToCreateMessage(String message)
        {
            super(message);
        }

        public FailedToCreateMessage(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    public class FailedToFindMessage extends Exception
    {
        public FailedToFindMessage(String message)
        {
            super(message);
        }

        public FailedToFindMessage(String message, Throwable cause)
        {
            super(message, cause);
        }
    }


    /** A simple class representing an email */
    public class MessageId
    {
        private final String id;
        private final String changeKey;

        private MessageId(String id, String changeKey)
        {
            this.id = id;
            this.changeKey = changeKey;
        }

        private String getId()
        {
            return id;
        }

        private String getChangeKey()
        {
            return changeKey;
        }

        @Override
        public String toString()
        {
            return "MessageId{" +
                   "id='" + id + '\'' +
                   ", changeKey='" + changeKey + '\'' +
                   '}';
        }
    }
}
