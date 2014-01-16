package com.cutlass.confluence.plugin.rest;

import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.PersonalInformation;
import com.atlassian.confluence.user.PersonalInformationManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.core.filters.ServletContextThreadLocal;
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
        final String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();
        final HashTranslator hashTranslator = HashTranslator.getInstance();

        final CacheControl NO_CACHE = new CacheControl();
        NO_CACHE.setNoStore(true);
        NO_CACHE.setNoCache(true);

        final String username = hashTranslator.getUsername(id.trim());
        final User user = userAccessor.getUser(username);

        final String filepath = userAccessor.getUserProfilePicture(user).getDownloadPath();
        final String filename = userAccessor.getUserProfilePicture(user).getFileName();

        String ct;

        BufferedImage image;

        if (filepath.contains("attachments"))
        {
            final PersonalInformation pi = personalInformationManager.getPersonalInformation(user);
            final Attachment attachment = attachmentManager.getAttachment(pi, filename);
            image = ImageIO.read(attachment.getContentsAsStream());
            ct = attachment.getContentType();

            if (ct.equals("image/pjpeg"))
            {
                ct = "image/jpeg";
            }
        }
        else
        {
            image = ImageIO.read(ServletContextThreadLocal.getContext().getResourceAsStream(filepath));
                    
            final int dotPos = filename.lastIndexOf(".");
            final String fileExtension = filename.substring(dotPos + 1);
            ct = "image/" + fileExtension;
        }

        final byte[] bytes = scale(image, size, size, ct);

        return Response.ok(bytes, ct).cacheControl(NO_CACHE).build();

    }

    public byte[] scale(final BufferedImage image, final int inHorizontal, final int inVertical, final String avatarType)
        throws IOException
    {

        final double scaleFactor = getScaleFactor(image.getWidth(), image.getHeight(), inHorizontal, inVertical);

        final BufferedImage image2 = new BufferedImage(image.getColorModel(), image.getRaster().createCompatibleWritableRaster(
            inHorizontal, inVertical), image.isAlphaPremultiplied(), null);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final Map<Key, Object> hintMap = new HashMap<Key, Object>();

        hintMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hintMap.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        hintMap.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        //hintMap.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        final AffineTransformOp op = new AffineTransformOp(AffineTransform.getScaleInstance(scaleFactor, scaleFactor),
            new RenderingHints(hintMap));

        ImageIO.write(op.filter(image, image2), avatarType.replaceAll("image/", ""), baos);

        baos.flush();
        final byte[] resultImageAsRawBytes = baos.toByteArray();
        baos.close();
        return resultImageAsRawBytes;
    }

    public void setAttachmentManager(final AttachmentManager attachmentManager)
    {
        this.attachmentManager = attachmentManager;
    }

    public void setPersonalInformationManager(final PersonalInformationManager personalInformationManager)
    {
        this.personalInformationManager = personalInformationManager;
    }

    public void setSettingsManager(final SettingsManager settingsManager)
    {
        this.settingsManager = settingsManager;
    }

    public void setUserAccessor(final UserAccessor userAccessor)
    {
        this.userAccessor = userAccessor;
    }

}
