package com.softartisans.timberwolf.integrated;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.List;

/**
 * An class for validating emails in an hbase result.
 */
public class EmailMatcher
{
    private List<FieldMatcher> matchers;

    private String columnFamily;

    public EmailMatcher(String columnFamily)
    {
        this.columnFamily = columnFamily;
    }

    /**
     * Checks whether this email matcher matches the row in the result
     * @param result the result from an hbase get or scan
     * @return true if it's a match, false otherwise
     */
    boolean matches(Result result)
    {
        return false;
    }

    public EmailMatcher Sender(final String alias)
    {
        matchers.add(new EmailAddressMatcher("Sender", alias));
        return this;
    }

    public EmailMatcher Subject(final String subject)
    {
        matchers.add(new FieldMatcher("Subject")
        {
            @Override
            protected boolean matches(String s)
            {
                return subject.equals(s);
            }
        });
        return this;
    }

    public EmailMatcher BodyContains(final String contents)
    {
        matchers.add(new FieldMatcher("Body")
        {
            @Override
            protected boolean matches(String s)
            {
                return s.contains(contents);
            }
        });
        return this;
    }

    private abstract class FieldMatcher
    {
        private String columnName;

        protected FieldMatcher(String columnName)
        {
            this.columnName = columnName;
        }

        public boolean matches(Result result)
        {
            String value = Bytes.toString(result.getValue(Bytes.toBytes(columnFamily),
                                                          Bytes.toBytes(columnName)));
            return value != null && matches(value);
        }

        protected abstract boolean matches(String s);

    }

    private final class EmailAddressMatcher extends FieldMatcher
    {
        final String prefix;

        private EmailAddressMatcher(final String column, final String alias)
        {
            super(column);
            this.prefix = alias + "@";
        }

        @Override
        protected boolean matches(String s)
        {
            return s.startsWith(prefix);
        }
    }
}
