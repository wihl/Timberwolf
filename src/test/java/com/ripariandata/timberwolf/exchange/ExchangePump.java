package com.ripariandata.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.CreateFolderResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.CreateFolderType;
import com.microsoft.schemas.exchange.services.x2006.messages.CreateItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.DeleteFolderType;
import com.microsoft.schemas.exchange.services.x2006.messages.DeleteItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.FolderInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.MoveItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.types.BodyTypeType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DisposalType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemResponseShapeType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageDispositionType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfAllItemsType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseFolderIdsType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseItemIdsType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfFoldersType;
import com.microsoft.schemas.exchange.services.x2006.types.TargetFolderIdType;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlsoap.schemas.soap.envelope.BodyType;
import org.xmlsoap.schemas.soap.envelope.EnvelopeDocument;
import org.xmlsoap.schemas.soap.envelope.EnvelopeType;

/**
 * This class puts a bunch of folders or emails into an account in exchange.
 * This should only be used by integration tests.
 */
public class ExchangePump
{

    private static final Logger LOG = LoggerFactory.getLogger(ExchangePump.class);
    private static final String DECLARATION = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    private static final String SOAP_ENCODING = "UTF-8";

    private String endpoint;
    private HttpUrlConnectionFactory connectionFactory = new SaslHttpUrlConnectionFactory();
    private String sender;

    public ExchangePump(final String exchangeUrl, final String senderEmail)
    {
        endpoint = exchangeUrl;
        sender = senderEmail;
    }

    private EnvelopeDocument createEmptyRequest(final String user)
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        envelope.addNewHeader().addNewExchangeImpersonation().addNewConnectingSID().setPrincipalName(user);
        return request;
    }

    /**
     * Tells Exchange to create all given 'folders' inside parentFolderId.
     * The request is performed as 'user'.
     * Will not create folders that already have ids.
     */
    private void createFolders(final String user, final TargetFolderIdType parentFolderId,
                               final String parentFolderName, final List<RequiredFolder> folders) throws FailedToCreateFolders
    {
        EnvelopeDocument request = createEmptyRequest(user);
        CreateFolderType createFolder = request.getEnvelope().addNewBody().addNewCreateFolder();
        createFolder.setParentFolderId(parentFolderId);
        NonEmptyArrayOfFoldersType requestedFolders = createFolder.addNewFolders();
        for (RequiredFolder folder : folders)
        {
            // Don't recreate folders that already exist
            if (folder.getId() == null)
            {
                LOG.debug("Creating folder: {}", folder.getName());
                requestedFolders.addNewFolder().setDisplayName(folder.getName());
            }
        }
        if (requestedFolders.sizeOfFolderArray() == 0)
        {
            LOG.debug("All requested folders were already created.");
            return;
        }
        CreateFolderResponseType response = sendRequest(request).getCreateFolderResponse();
        FolderInfoResponseMessageType[] array = response.getResponseMessages().getCreateFolderResponseMessageArray();
        int i = 0;
        for (FolderInfoResponseMessageType folderResponse : array)
        {
            for (FolderType folder : folderResponse.getFolders().getFolderArray())
            {
                while (folders.get(i).getId() != null)
                {
                    i++;
                }
                folders.get(i).setId(folder.getFolderId().getId());
                i++;
            }
        }
        if (i < folders.size())
        {
            LOG.error("Failed to create all folders for user: {}", user);
            LOG.error("Requested Folders in {}:", parentFolderName);
            for (RequiredFolder folder : folders)
            {
                LOG.error("  {}", folder.getName());
            }
            throw new FailedToCreateFolders("Failed to create all folders, perhaps some existed before running the test");
        }
    }

    public void createFolders(final String user, final String parent, final List<RequiredFolder> folders)
            throws FailedToCreateFolders
    {
        TargetFolderIdType parentFolder = TargetFolderIdType.Factory.newInstance();
        parentFolder.addNewFolderId().setId(parent);
        createFolders(user, parentFolder, parent, folders);
    }

    public void createFolders(final String user, final DistinguishedFolderIdNameType.Enum parent,
                              final List<RequiredFolder> folders) throws FailedToCreateFolders
    {
        TargetFolderIdType parentFolder = TargetFolderIdType.Factory.newInstance();
        parentFolder.addNewDistinguishedFolderId().setId(parent);
        createFolders(user, parentFolder, parent.toString(), folders);
    }

    public void deleteFolders(final String user, final List<RequiredFolder> folders) throws FailedToDeleteMessage
    {
        EnvelopeDocument request = createEmptyRequest(user);
        DeleteFolderType deleteFolder = request.getEnvelope().addNewBody().addNewDeleteFolder();
        deleteFolder.setDeleteType(DisposalType.HARD_DELETE);
        NonEmptyArrayOfBaseFolderIdsType doomedFolders = deleteFolder.addNewFolderIds();

        for (RequiredFolder folder : folders)
        {
            final String folderId = folder.getId();
            if (folderId != null && folderId.length() > 0)
            {
                doomedFolders.addNewFolderId().setId(folderId);
                LOG.debug("Preparing to delete folder: {}", folderId);
            }
        }
        BodyType response = sendRequest(request);
        ResponseMessageType[] responses =
                response.getDeleteFolderResponse().getResponseMessages().getDeleteFolderResponseMessageArray();
        for (ResponseMessageType resp : responses)
        {
            if (resp.getResponseCode() != ResponseCodeType.NO_ERROR)
            {
                LOG.debug(response.toString());
                throw new FailedToDeleteMessage(
                        "ResponseCode some sort of error: " + resp.getResponseCode());
            }
        }
    }

    public void deleteEmails(final String user, final List<MessageId> emails) throws FailedToDeleteMessage
    {
        EnvelopeDocument request = createEmptyRequest(user);
        DeleteItemType deleteItem = request.getEnvelope().addNewBody().addNewDeleteItem();
        deleteItem.setDeleteType(DisposalType.HARD_DELETE);
        NonEmptyArrayOfBaseItemIdsType doomedItems = deleteItem.addNewItemIds();
        for (MessageId email : emails)
        {
            String emailId = email.getId();
            doomedItems.addNewItemId().setId(emailId);
            LOG.debug("Preparing to delete email item: {}", emailId);
        }
        BodyType response = sendRequest(request);
        ResponseMessageType[] responses =
                response.getDeleteItemResponse().getResponseMessages().getDeleteItemResponseMessageArray();
        for (ResponseMessageType resp : responses)
        {
            if (resp.getResponseCode() != ResponseCodeType.NO_ERROR)
            {
                LOG.debug(response.toString());
                throw new FailedToDeleteMessage(
                        "ResponseCode some sort of error: " + resp.getResponseCode());
            }
        }
    }

    private void createEmails(final List<RequiredEmail> emails, final EnvelopeDocument request,
                              final NonEmptyArrayOfAllItemsType emptyItemsType)
            throws FailedToCreateMessage
    {
        for (RequiredEmail email : emails)
        {
            MessageType exchangeMessage = emptyItemsType.addNewMessage();
            exchangeMessage.addNewFrom().addNewMailbox().setEmailAddress(sender);
            com.microsoft.schemas.exchange.services.x2006.types.BodyType body = exchangeMessage.addNewBody();
            body.setBodyType(BodyTypeType.TEXT);
            body.setStringValue(email.getBody());
            exchangeMessage.setSubject(email.getSubject());
            exchangeMessage.addNewToRecipients().addNewMailbox().setEmailAddress(email.getTo());
            if (email.getCc() != null)
            {
                exchangeMessage.addNewCcRecipients().addNewMailbox().setEmailAddress(email.getCc());
            }
            if (email.getBcc() != null)
            {
                exchangeMessage.addNewBccRecipients().addNewMailbox().setEmailAddress(email.getBcc());
            }
        }
        BodyType response = sendRequest(request);
        ItemInfoResponseMessageType[] responses =
                response.getCreateItemResponse().getResponseMessages().getCreateItemResponseMessageArray();
        if (responses.length != emails.size())
        {
            LOG.debug(response.toString());
            throw new FailedToCreateMessage(
                    "There should have been " + emails.size() + " response message to createItem");
        }
        for (ItemInfoResponseMessageType resp : responses)
        {
            if (resp.getResponseCode() != ResponseCodeType.NO_ERROR)
            {
                LOG.debug(response.toString());
                throw new FailedToCreateMessage(
                        "ResponseCode some sort of error: " + resp.getResponseCode());
            }
        }
    }

    public void sendMessages(final List<RequiredEmail> emails) throws FailedToCreateMessage
    {
        EnvelopeDocument request = createEmptyRequest(sender);
        CreateItemType createItem = request.getEnvelope().addNewBody().addNewCreateItem();
        createItem.setMessageDisposition(MessageDispositionType.SEND_ONLY);
        NonEmptyArrayOfAllItemsType items = createItem.addNewItems();

        createEmails(emails, request, items);
    }


    public void saveDrafts(final String user, final List<RequiredEmail> drafts) throws FailedToCreateMessage
    {
        EnvelopeDocument request = createEmptyRequest(user);
        CreateItemType createItem = request.getEnvelope().addNewBody().addNewCreateItem();
        createItem.addNewSavedItemFolderId().addNewDistinguishedFolderId().setId(DistinguishedFolderIdNameType.DRAFTS);
        createItem.setMessageDisposition(MessageDispositionType.SAVE_ONLY);
        NonEmptyArrayOfAllItemsType items = createItem.addNewItems();

        createEmails(drafts, request, items);
    }

    public void sendAndSave(final String user, final List<RequiredEmail> drafts) throws FailedToCreateMessage
    {
        EnvelopeDocument request = createEmptyRequest(user);
        CreateItemType createItem = request.getEnvelope().addNewBody().addNewCreateItem();
        createItem.addNewSavedItemFolderId().addNewDistinguishedFolderId()
                  .setId(DistinguishedFolderIdNameType.SENTITEMS);
        createItem.setMessageDisposition(MessageDispositionType.SEND_AND_SAVE_COPY);
        NonEmptyArrayOfAllItemsType items = createItem.addNewItems();

        createEmails(drafts, request, items);
    }


    public HashMap<String, List<MessageId>> findItems(final String user,
                                                      final DistinguishedFolderIdNameType.Enum parent)
            throws FailedToFindMessage
    {
        HashMap<String, List<MessageId>> emailResults = new HashMap<String, List<MessageId>>();
        EnvelopeDocument request = createEmptyRequest(user);
        FindItemType findItem = request.getEnvelope().addNewBody().addNewFindItem();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        ItemResponseShapeType itemShape = findItem.addNewItemShape();
        itemShape.setBaseShape(DefaultShapeNamesType.DEFAULT);
        // I tried to use ID_ONLY and add some AdditionalProperties, but the
        // schema appears to be screwy and not have FieldURI in there correctly
        findItem.addNewParentFolderIds().addNewDistinguishedFolderId().setId(parent);
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

    private void moveMessages(final String user, final TargetFolderIdType targetFolderId,
                              final List<MessageId> messageIds) throws FailedToMoveMessage
    {
        EnvelopeDocument request = createEmptyRequest(user);
        MoveItemType moveItem = request.getEnvelope().addNewBody().addNewMoveItem();
        moveItem.setToFolderId(targetFolderId);
        NonEmptyArrayOfBaseItemIdsType itemIds = moveItem.addNewItemIds();
        for (MessageId messageId : messageIds)
        {
            ItemIdType itemId = itemIds.addNewItemId();
            itemId.setId(messageId.getId());
            itemId.setChangeKey(messageId.getChangeKey());
        }
        BodyType response = sendRequest(request);
        ItemInfoResponseMessageType[] responses =
                response.getMoveItemResponse().getResponseMessages().getMoveItemResponseMessageArray();
        if (responses.length != messageIds.size())
        {
            throw new FailedToMoveMessage("There should have been 1 response message to moveItem");
        }

        for (ItemInfoResponseMessageType resp : responses)
        {
            if (resp.getResponseCode() != ResponseCodeType.NO_ERROR)
            {
                throw new FailedToMoveMessage("ResponseCode some sort of error: " + resp.getResponseCode());
            }
        }
    }

    public void moveMessages(final String user, final String folder, final List<MessageId> messageIds)
            throws FailedToMoveMessage
    {
        TargetFolderIdType targetFolderId = TargetFolderIdType.Factory.newInstance();
        targetFolderId.addNewFolderId().setId(folder);
        moveMessages(user, targetFolderId, messageIds);
    }

    public void moveMessages(final String user, final DistinguishedFolderIdNameType.Enum distinguishedFolder,
                             final List<MessageId> messageIds) throws FailedToMoveMessage
    {
        TargetFolderIdType targetFolderId = TargetFolderIdType.Factory.newInstance();
        targetFolderId.addNewDistinguishedFolderId().setId(distinguishedFolder);
        moveMessages(user, targetFolderId, messageIds);
    }

    private BodyType sendRequest(final EnvelopeDocument envelope) throws SendRequestFailed
    {
        String request = DECLARATION + envelope.xmlText();

        LOG.trace("Sending SOAP request to {}.  SOAP envelope:", endpoint);
        LOG.trace(envelope.toString());

        try
        {
            HttpURLConnection conn = connectionFactory.newInstance(endpoint, request.getBytes(SOAP_ENCODING));

            int code = conn.getResponseCode();

            InputStream responseData = conn.getInputStream();

            int amtAvailable = responseData.available();

            if (code == HttpURLConnection.HTTP_OK)
            {
                EnvelopeDocument response = EnvelopeDocument.Factory.parse(responseData);

                LOG.trace("SOAP response received from {}.  SOAP envelope:", endpoint);
                LOG.trace(response.toString());

                return response.getEnvelope().getBody();
            }
            else
            {
                LOG.debug("HTTP Error: {}", code);
                throw new HttpErrorException(code);
            }
        }
        catch (Exception e)
        {
            throw new SendRequestFailed("Unexpected Exception: " + e.getMessage(), e);
        }

    }

    /** Exception for when sending a request to exchange fails. */
    public class SendRequestFailed extends RuntimeException
    {
        public SendRequestFailed(final String message, final Throwable cause)
        {
            super(message, cause);
        }
    }

    /** Exception for when creating folders fails. */
    public class FailedToCreateFolders extends Exception
    {
        public FailedToCreateFolders(final String message)
        {
            super(message);
        }
    }

    /** Exception for when creating messages fails. */
    public class FailedToCreateMessage extends Exception
    {
        public FailedToCreateMessage(final String message)
        {
            super(message);
        }
    }

    /** Exception for when deleting messages fails. */
    public class FailedToDeleteMessage extends Exception
    {
        public FailedToDeleteMessage(final String message)
        {
            super(message);
        }
    }

    /** Exception for when finding messages fails. */
    public class FailedToFindMessage extends Exception
    {
        public FailedToFindMessage(final String message)
        {
            super(message);
        }
    }

    /** Exception for when moving messages fails. */
    public class FailedToMoveMessage extends Exception
    {
        public FailedToMoveMessage(final String message)
        {
            super(message);
        }
    }


    /** A simple class representing an email. */
    public final class MessageId
    {
        private final String id;
        private final String changeKey;

        private MessageId(final String messageId, final String messageChangeKey)
        {
            id = messageId;
            changeKey = messageChangeKey;
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
            return "MessageId{"
                   + "id='" + id + '\''
                   + ", changeKey='" + changeKey + '\''
                   + '}';
        }
    }
}
