package com.softartisans.timberwolf.integrated;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

/** A class for validating emails in an hbase result. */
public class EmailMatcher
{
    private List<FieldMatcher> matchers;

    private String family;

    public EmailMatcher(final String columnFamily)
    {
        matchers = new ArrayList<FieldMatcher>();
        this.family = columnFamily;
    }

    /**
     * Checks whether this email matcher matches the row in the result.
     *
     * @param result the result from an hbase get or scan
     * @return true if it's a match, false otherwise
     */
    boolean matches(final Result result)
    {
        for (FieldMatcher matcher : matchers)
        {
            if (!matcher.matches(result))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Require the sender to have the given alias.
     * @param alias the alias of the sender
     * @return this
     */
    public EmailMatcher sender(final String alias)
    {
        matchers.add(new EmailAddressMatcher("Sender", alias));
        return this;
    }

    /**
     * Require the to field to have the given alias.
     * @param alias the alias of the sender
     * @return this
     */
    public EmailMatcher to(final String alias)
    {
        matchers.add(new EmailAddressMatcher("To", alias));
        return this;
    }

    /**
     * Require the cc to have the given alias.
     * @param alias the alias of the sender
     * @return this
     */
    public EmailMatcher cc(final String alias)
    {
        matchers.add(new EmailAddressMatcher("Cc", alias));
        return this;
    }

    /**
     * Require the bcc to have the given alias.
     * @param alias the alias of the sender
     * @return this
     */
    public EmailMatcher bcc(final String alias)
    {
        matchers.add(new EmailAddressMatcher("Bcc", alias));
        return this;
    }

    /**
     * Require the subject to be the given value.
     * @param subject the expected subject
     * @return this
     */
    public EmailMatcher subject(final String subject)
    {
        matchers.add(new FieldMatcher("Subject")
        {
            @Override
            protected boolean matches(final String s)
            {
                return subject.equals(s);
            }

            @Override
            public void appendToStringBuilder(final StringBuilder sb)
            {
                sb.append("Subject is \"");
                sb.append(subject);
                sb.append("\"");
            }
        });
        return this;
    }

    /**
     * Require the body to contain the given contents.
     * @param contents some text that is expected in the body
     * @return this
     */
    public EmailMatcher bodyContains(final String contents)
    {
        matchers.add(new FieldMatcher("Body")
        {
            @Override
            protected boolean matches(final String s)
            {
                return s.contains(contents);
            }

            @Override
            public void appendToStringBuilder(final StringBuilder sb)
            {
                sb.append("Body contains \"");
                sb.append(contents);
                sb.append("\"");
            }
        });
        return this;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Email with: ");
        for (FieldMatcher matcher : matchers)
        {
            matcher.appendToStringBuilder(sb);
            sb.append(", ");
        }
        return sb.substring(0, sb.length() - 2);
    }

    /**
     * Match a single field.
     */
    private abstract class FieldMatcher
    {
        private String columnName;

        protected String getColumnName()
        {
            return columnName;
        }

        protected FieldMatcher(final String aColumnName)
        {
            this.columnName = aColumnName;
        }

        public boolean matches(final Result result)
        {
            String value = Bytes.toString(result.getValue(Bytes.toBytes(family),
                                                          Bytes.toBytes(columnName)));
            return value != null && matches(value);
        }

        protected abstract boolean matches(String s);

        public abstract void appendToStringBuilder(StringBuilder sb);
    }

    /**
     * Match an email address field, such as To or Sender.
     */
    private final class EmailAddressMatcher extends FieldMatcher
    {
        private String prefix;

        private EmailAddressMatcher(final String column, final String alias)
        {
            super(column);
            this.prefix = alias + "@";
        }

        @Override
        protected boolean matches(final String s)
        {
            return s.startsWith(prefix);
        }

        @Override
        public void appendToStringBuilder(final StringBuilder sb)
        {
            sb.append(getColumnName());
            sb.append(" begins \"");
            sb.append(prefix);
            sb.append("\"");

        }
    }
}
