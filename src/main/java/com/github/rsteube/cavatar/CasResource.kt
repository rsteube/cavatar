package com.github.rsteube.cavatar

import com.atlassian.confluence.user.ConfluenceUser
import com.atlassian.confluence.user.UserAccessor
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

@Path("/avatar")
class CasResource {
    @GET
    @Path("server/{id}")
    @Throws(IOException::class)
    fun getUsersPlus(@PathParam("id") id: String, @QueryParam("s") @DefaultValue("48") size: Int): Response {
        LOG.debug("Image requested for [hash: $id, size: $size x $size]")

        val hashString = id.split("\\.").toTypedArray()[0]
        val username = HashTranslator.getUsername(hashString.trim { it <= ' ' })
        LOG.debug("The user associated with the email hash is: $username")

        val (image, contentType) = profilePicture(userAccessor().getUserByName(username)) ?: defaultPicture().also {
            LOG.debug("Using defaultPicture for $username")
        }
        val bytes = image.scale(size, size, contentType)
        return Response.ok(bytes, contentType).cacheControl(NO_CACHE).build()
    }

    private fun userAccessor() = ContainerManager.getComponent("userAccessor") as UserAccessor
    private fun profilePicture(user: ConfluenceUser?) = userAccessor().getUserProfilePicture(user)?.let { info -> info.bytes?.let { bytes -> ImageIO.read(bytes)?.let { it to info.contentType.replace("pjpeg", "jpeg") } } }
    private fun defaultPicture() = ImageIO.read(javaClass.classLoader.getResourceAsStream("/images/anonymous.png")) to "image/png"

    private fun getScaleFactor(width: Int, height: Int, maxTargetWidth: Int, maxTargetHeight: Int) =
            (maxTargetWidth.toDouble() / width.toDouble()).coerceAtMost(maxTargetHeight.toDouble() / height.toDouble())

    @Throws(IOException::class)
    fun BufferedImage.scale(inHorizontal: Int, inVertical: Int, avatarType: String): ByteArray {
        val scaleFactor = getScaleFactor(width, height, inHorizontal, inVertical)
        val image2 = BufferedImage(colorModel, raster.createCompatibleWritableRaster(
                inHorizontal, inVertical), isAlphaPremultiplied, null)
        val baos = ByteArrayOutputStream()

        val op = AffineTransformOp(AffineTransform.getScaleInstance(scaleFactor, scaleFactor),
                RenderingHints(mapOf(
                        KEY_ANTIALIASING to VALUE_ANTIALIAS_ON,
                        KEY_COLOR_RENDERING to VALUE_COLOR_RENDER_QUALITY,
                        KEY_INTERPOLATION to VALUE_INTERPOLATION_BICUBIC
                )))
        ImageIO.write(op.filter(this, image2), avatarType.replace("image/", ""), baos)
        baos.flush()
        val resultImageAsRawBytes = baos.toByteArray()
        baos.close()
        return resultImageAsRawBytes
    }

    companion object {
        val NO_CACHE = CacheControl().apply {
            isNoStore = true
            isNoCache = true
        }
        val LOG = Logger.getLogger(CasResource::class.java)
    }
}