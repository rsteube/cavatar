package com.cutlass.confluence.plugin.rest;

import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.PersonalInformationManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.user.actions.ProfilePictureInfo;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.user.User;

@Path("/avatar")
@AnonymousAllowed
public class CasResource
{

    AttachmentManager attachmentManager;

    Logger log = Logger.getLogger(CasResource.class);

    PersonalInformationManager personalInformationManager;

    SettingsManager settingsManager;

    UserAccessor userAccessor;

    private double getScaleFactor(final int width, final int height, final int maxTargetWidth, final int maxTargetHeight)
    {
        final double widthScaleFactor = (double)maxTargetWidth / (double)width;
        final double heightScaleFactor = (double)maxTargetHeight / (double)height;
        final double scaleFactor = Math.min(widthScaleFactor, heightScaleFactor);
        return scaleFactor;
    }

    @GET
    @Path("server/{id}")
    public Response getUsersPlus(@PathParam("id")
    final String id, @QueryParam("s")
    @DefaultValue("48")
    final int size) throws IOException
    {
        log.debug("Image requested for email hash: " + id);
        log.debug("The requested size is: " + size + " x " + size);
        // final String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();

        final HashTranslator hashTranslator = HashTranslator.getInstance();

        final CacheControl NO_CACHE = new CacheControl();
        NO_CACHE.setNoStore(true);
        NO_CACHE.setNoCache(true);

        String hashString = id.split("\\.")[0];
        final String username = hashTranslator.getUsername(hashString.trim());

        log.debug("The user associated with the email hash is: " + username);

        final User user = userAccessor.getUser(username);

        final ProfilePictureInfo profilePictureInfo = userAccessor.getUserProfilePicture(user);

        BufferedImage image = ImageIO.read(profilePictureInfo.getBytes());
        String ct = profilePictureInfo.getContentType();
        if (ct.equals("image/pjpeg"))
        {
            ct = "image/jpeg";
        }

        byte[] bytes = null;
        if (image != null)
        {
            log.debug("The image was found and is being scaled appropriatly");
            bytes = scale(image, size, size, ct);
            log.debug("The image has been scaled");
        }
        else
        {
            log.debug("The image was not found");
            InputStream defaultImage = getClass().getClassLoader().getResourceAsStream("/images/anonymous.png");
            image = ImageIO.read(defaultImage);
            bytes = scale(image, size, size, "image/png");
            log.debug("The anonymous image has been scaled");
        }

        return Response.ok(bytes, ct).cacheControl(NO_CACHE).build();

    }

    public byte[] scale(final BufferedImage image, final int inHorizontal, final int inVertical, final String avatarType)
        throws IOException
    {

        final double scaleFactor = getScaleFactor(image.getWidth(), image.getHeight(), inHorizontal, inVertical);

        final BufferedImage image2 = new BufferedImage(image.getColorModel(),
            image.getRaster().createCompatibleWritableRaster(inHorizontal, inVertical), image.isAlphaPremultiplied(), null);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final Map<Key, Object> hintMap = new HashMap<Key, Object>();

        hintMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hintMap.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        hintMap.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        // hintMap.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        final AffineTransformOp op = new AffineTransformOp(AffineTransform.getScaleInstance(scaleFactor, scaleFactor),
            new RenderingHints(hintMap));

        ImageIO.write(op.filter(image, image2), avatarType.replaceAll("image/", ""), baos);

        baos.flush();
        final byte[] resultImageAsRawBytes = baos.toByteArray();
        baos.close();
        return resultImageAsRawBytes;
    }

    public void setAttachmentManager(final AttachmentManager attachmentManagerParam)
    {
        attachmentManager = attachmentManagerParam;
    }

    public void setPersonalInformationManager(final PersonalInformationManager personalInformationManagerParam)
    {
        personalInformationManager = personalInformationManagerParam;
    }

    public void setSettingsManager(final SettingsManager settingsManagerParam)
    {
        settingsManager = settingsManagerParam;
    }

    public void setUserAccessor(final UserAccessor userAccessorParam)
    {
        userAccessor = userAccessorParam;
    }

}
