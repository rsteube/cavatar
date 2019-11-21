package com.github.rsteube.cavatar

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal
import com.atlassian.confluence.user.UserAccessor
import com.atlassian.plugins.rest.common.security.AnonymousAllowed
import com.atlassian.spring.container.ContainerManager
import org.apache.log4j.Logger
import java.awt.RenderingHints
import java.awt.RenderingHints.*
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO
import javax.ws.rs.*
import javax.ws.rs.core.CacheControl
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.UNAUTHORIZED

@Path("/avatar")
@AnonymousAllowed
class CasResource {
    private var log = Logger.getLogger(CasResource::class.java)
    private fun getScaleFactor(width: Int, height: Int, maxTargetWidth: Int, maxTargetHeight: Int): Double {
        val widthScaleFactor = maxTargetWidth.toDouble() / width.toDouble()
        val heightScaleFactor = maxTargetHeight.toDouble() / height.toDouble()
        return widthScaleFactor.coerceAtMost(heightScaleFactor)
    }

    @GET
    @Path("server/{id}")
    @Throws(IOException::class)
    fun getUsersPlus(@PathParam("id") id: String, @QueryParam("s") @DefaultValue("48") size: Int): Response {
        if (AuthenticatedUserThreadLocal.get() == null) return Response.status(UNAUTHORIZED).build()

        log.debug("Image requested for email hash: $id")
        log.debug("The requested size is: $size x $size")
        // final String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();
        val NO_CACHE = CacheControl()
        NO_CACHE.isNoStore = true
        NO_CACHE.isNoCache = true
        val hashString = id.split("\\.").toTypedArray()[0]
        val username = HashTranslator.getUsername(hashString.trim { it <= ' ' })
        log.debug("The user associated with the email hash is: $username")
        val userAccessor = ContainerManager.getComponent("userAccessor") as UserAccessor
        val user = userAccessor.getUser(username)
        val profilePictureInfo = userAccessor.getUserProfilePicture(user)
        val ct = profilePictureInfo.contentType.replace("image/pjpeg", "image/jpeg")

        var image = ImageIO.read(profilePictureInfo.bytes)
        var bytes: ByteArray? = null
        if (image != null) {
            log.debug("The image was found and is being scaled appropriatly")
            bytes = scale(image, size, size, ct)
            log.debug("The image has been scaled")
        } else {
            log.debug("The image was not found")
            val defaultImage = javaClass.classLoader.getResourceAsStream("/images/anonymous.png")
            image = ImageIO.read(defaultImage)
            bytes = scale(image, size, size, "image/png")
            log.debug("The anonymous image has been scaled")
        }
        return Response.ok(bytes, ct).cacheControl(NO_CACHE).build()
    }

    @Throws(IOException::class)
    fun scale(image: BufferedImage?, inHorizontal: Int, inVertical: Int, avatarType: String): ByteArray {
        val scaleFactor = getScaleFactor(image!!.width, image.height, inHorizontal, inVertical)
        val image2 = BufferedImage(image.colorModel, image.raster.createCompatibleWritableRaster(
                inHorizontal, inVertical), image.isAlphaPremultiplied, null)
        val baos = ByteArrayOutputStream()
        // hintMap.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        val op = AffineTransformOp(AffineTransform.getScaleInstance(scaleFactor, scaleFactor),
                RenderingHints(mapOf(
                        KEY_ANTIALIASING to VALUE_ANTIALIAS_ON,
                        KEY_COLOR_RENDERING to VALUE_COLOR_RENDER_QUALITY,
                        KEY_INTERPOLATION to VALUE_INTERPOLATION_BICUBIC
                )))
        ImageIO.write(op.filter(image, image2), avatarType.replace("image/".toRegex(), ""), baos)
        baos.flush()
        val resultImageAsRawBytes = baos.toByteArray()
        baos.close()
        return resultImageAsRawBytes
    }
}