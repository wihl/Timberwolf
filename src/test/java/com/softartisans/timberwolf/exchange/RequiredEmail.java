package com.softartisans.timberwolf.exchange;


/** Helper class for required emails in exchange */
public class RequiredEmail
{
    private String subject;
    private String body;
    private String folderId;
    private String to;

    public RequiredEmail(String subjectText, String bodyText)
    {
        subject = subjectText;
        body = bodyText;
    }

    public void initialize(RequiredFolder folder, String username)
    {
        if (folderId != null)
        {
            throw new UnsupportedOperationException("folder can only be set once on email");
        }
        folderId = folder.getId();
    }

    public String getSubject()
    {
        if (folderId == null)
        {
            throw new UnsupportedOperationException("folder must be set on email");
        }
        return subject + "::" + folderId;
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

}
