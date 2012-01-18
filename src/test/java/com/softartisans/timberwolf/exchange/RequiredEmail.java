package com.softartisans.timberwolf.exchange;


import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;

/** Helper class for required emails in exchange */
public class RequiredEmail
{
    public static final String DISTINGUISHED_FOLDER_PREFIX = "Distinguished -- ";
    public static final String FOLDER_ID_SEPARATOR = "::";
    private String subject;
    private String body;
    private String folderId;
    private String to;

    public RequiredEmail(String toEmail, String subjectText, String bodyText)
    {
        subject = subjectText;
        body = bodyText;
        to = toEmail;
    }

    public void initialize(RequiredFolder folder, String username)
    {
        if (folderId != null)
        {
            throw new UnsupportedOperationException("folder can only be set once on email");
        }
        folderId = folder.getId();
    }

    public void initialize(final DistinguishedFolderIdNameType.Enum folder)
    {
        if (folderId != null)
        {
            throw new UnsupportedOperationException("folder can only be set once on email");
        }
        folderId = DISTINGUISHED_FOLDER_PREFIX + folder;
    }

    public String getSubject()
    {
        if (folderId == null)
        {
            throw new UnsupportedOperationException("folder must be set on email");
        }
        return subject + FOLDER_ID_SEPARATOR + folderId;
    }

    public String getBody()
    {
        if (body == null)
        {
            return "The body for the email with a subject: " + getSubject();
        }
        return body;
    }

    public String getTo()
    {
        return to;
    }

    public static String getFolderId(String subject)
    {
        String[] s = subject.split(FOLDER_ID_SEPARATOR);
        if (s.length != 2)
        {
            return null;
        }
        else
        {
            if (s[1].startsWith(DISTINGUISHED_FOLDER_PREFIX))
            {
                return s[1].substring(DISTINGUISHED_FOLDER_PREFIX.length());
            }
            return s[1];
        }

    }
}
