package com.cutlass.confluence.plugin.rest;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.User;
import com.atlassian.user.search.page.PagerUtils;
import com.opensymphony.util.TextUtils;

/**
 * The Class HashTranslator
 */
public final class HashTranslator
{
    /** The instance. */
    private static HashTranslator instance = new HashTranslator();

    public static HashTranslator getInstance()
    {
        return instance;
    }

    Logger log = Logger.getLogger(this.getClass());

    Map<String, String> translation = new HashMap<String, String>();

    UserAccessor userAccessor;

    private HashTranslator()
    {
        userAccessor = (UserAccessor) ContainerManager.getInstance().getContainerContext().getComponent("userAccessor");
        populateTranslation();
    }

    private String getEmailHash(final String email) {
        String emailHash = null;

        if (email != null) {
                try {
                    final MessageDigest md = MessageDigest.getInstance("MD5");
                    final byte[] input = md.digest(email.trim().getBytes("CP1252"));
                    final StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < input.length; ++i) {
                        sb.append(Integer.toHexString((input[i] & 0xFF) | 0x100).substring(1,3));
                    }
                    emailHash = sb.toString();
                } catch (final NoSuchAlgorithmException nsae) {
                    log.warn("Unable to generate gravatar ID for email '" + email + "'."  + nsae);
                } catch (final UnsupportedEncodingException uee) {
                    log.warn("Unable to generate gravatar ID for email '" + email + "'."  + uee);
                }
        }

        return emailHash;
    }

    public String getUsername(final String emailhash)
    {

        if (translation.keySet().size() != PagerUtils.toList(userAccessor.getUsers()).size())
        {
            populateTranslation();
        }

        String username = translation.get(emailhash);

        if (!TextUtils.stringSet(username))
        {
            username = "anonymous";
        }

        return username;
    }

    private void populateTranslation()
    {

        for (final Object element : PagerUtils.toList(userAccessor.getUsers()))
        {
            final User user = (User)element;
            translation.put(getEmailHash(user.getEmail().toLowerCase()), user.getName());

        }

    }

}
