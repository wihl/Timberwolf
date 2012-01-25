package com.ripariandata.timberwolf.exchange;


import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class for required emails in exchange. */
public class RequiredEmail
{
    private static final Logger LOG = LoggerFactory.getLogger(RequiredEmail.class);
    public static final String DISTINGUISHED_FOLDER_PREFIX = "Distinguished -- ";
    public static final String FOLDER_ID_SEPARATOR = "::";
    private String subject;
    private String body;
    private String folderId;
    private String to;
    private String cc;
    private String bcc;
    private String from;

    public RequiredEmail(final String toEmail, final String subjectText, final String bodyText)
    {
        subject = subjectText;
        body = bodyText;
        to = toEmail;
    }

    public void initialize(final RequiredFolder folder, final String username)
    {
        if (folderId != null)
        {
            throw new UnsupportedOperationException("folder can only be set once on email: " + toString());
        }
        folderId = folder.getId();
        LOG.debug("New email in {}: {}", folder.getName(), toString());
    }

    public void initialize(final DistinguishedFolderIdNameType.Enum folder)
    {
        if (folderId != null)
        {
            throw new UnsupportedOperationException("folder can only be set once on email: " + toString());
        }
        folderId = DISTINGUISHED_FOLDER_PREFIX + folder;
    }

    public String getSubject()
    {
        if (folderId == null)
        {
            throw new UnsupportedOperationException("folder must be set on email: " + toString());
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

    public String getToString()
    {
        return to + ";";
    }

    public String getFrom()
    {
        return from;
    }

    public String getCc()
    {
        return cc;
    }

    public String getCcString()
    {
        return cc + ";";
    }

    public String getBcc()
    {
        return bcc;
    }

    public String getBccString()
    {
        return bcc + ";";
    }

    public static String getFolderId(final String subject)
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

    public RequiredEmail cc(final String ccEmail)
    {
        cc = ccEmail;
        return this;
    }

    public RequiredEmail bcc(final String bccEmail)
    {
        bcc = bccEmail;
        return this;
    }

    public RequiredEmail to(final String toEmail)
    {
        to = toEmail;
        return this;
    }

    /**
     * The checked sender of the given email.
     * This does not change the sender.
     *
     * @param fromEmail the expected sender
     * @return this
     */
    public RequiredEmail from(final String fromEmail)
    {
        from = fromEmail;
        return this;
    }

    @Override
    public String toString()
    {
        return "RequiredEmail{"
               + "subject='" + subject + '\''
               + ", body='" + body + '\''
               + ", folderId='" + folderId + '\''
               + ", to='" + to + '\''
               + ", cc='" + cc + '\''
               + ", bcc='" + bcc + '\''
               + ", from='" + from + '\''
               + '}';
    }
}
