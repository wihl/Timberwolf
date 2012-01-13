package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderType;

import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains helper methods for FindFolder requests.
 */
public final class FindFolderHelper
{

    private static final Logger LOG = LoggerFactory.getLogger(FindFolderHelper.class);

    /**
     * Enforces not being able to create an instance.
     */
    private FindFolderHelper()
    {

    }

    /**
     * Creates a FindFolderType for the given distinguished folder.
     *
     * @param folder The distinguished folder.
     * @return The FindItemType for the distinguished folder.
     */
    static FindFolderType getFindFoldersRequest(final DistinguishedFolderIdNameType.Enum folder)
    {
        FindFolderType findFolder = getFindFolderType();

        DistinguishedFolderIdType folderId = findFolder.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(folder);

        return findFolder;
    }

    /**
     * Creates a FindFolderType for the given folder.
     *
     * @param folderId The folder id for the folder from which to get ids.
     * @return The FindFolderType for the given folder..
     */
    static FindFolderType getFindFoldersRequest(final String folderId)
    {
        FindFolderType findFolder = getFindFolderType();

        FolderIdType folder = findFolder.addNewParentFolderIds().addNewFolderId();
        folder.setId(folderId);

        return findFolder;
    }

    /**
     * Creates a FindFolderType with a default behavior. The FolderQueryTraversal is set to deep.
     * @return A FindFolderType.
     */
    private static FindFolderType getFindFolderType()
    {
        FindFolderType findFolder = FindFolderType.Factory.newInstance();
        findFolder.setTraversal(FolderQueryTraversalType.DEEP);
        findFolder.addNewFolderShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);

        return findFolder;
    }

    /**
     * Gets a list of folder ids for the folder specified by findFolder.
     *
     * @param exchangeService The Exchange service to use.
     * @param findFolder The FindFolder request to use.
     * @param targetUser The user to impersonate during the Exchange FindFolder request.
     * @return A queue of all child folders.
     * @throws ServiceCallException If the Exchange service could not be connected to.
     * @throws HttpErrorException If the response could not be parsed.
     */
    static Queue<String> findFolders(final ExchangeService exchangeService, final FindFolderType findFolder,
                                     final String targetUser)
            throws ServiceCallException, HttpErrorException
    {
        FindFolderResponseType response = exchangeService.findFolder(findFolder, targetUser);

        if (response == null)
        {
            LOG.debug("Exchange service returned null find folder response.");
            throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Null response from Exchange service.");
        }

        ArrayOfResponseMessagesType array = response.getResponseMessages();
        Queue<String> folders = new LinkedList<String>();
        for (FindFolderResponseMessageType message : array.getFindFolderResponseMessageArray())
        {
            ResponseCodeType.Enum errorCode = message.getResponseCode();
            if (errorCode != null && errorCode != ResponseCodeType.NO_ERROR)
            {
                LOG.debug(errorCode.toString());
                throw new ServiceCallException(errorCode, "SOAP response contained an error.");
            }

            if (message.isSetRootFolder() && message.getRootFolder().isSetFolders())
            {
                for (FolderType folder : message.getRootFolder().getFolders().getFolderArray())
                {
                    if (folder.isSetFolderId())
                    {
                        folders.add(folder.getFolderId().getId());
                    }
                }
            }
        }
        return folders;
    }
}
