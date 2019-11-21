package com.github.rsteube.cavatar

import com.atlassian.confluence.user.UserAccessor
import com.atlassian.spring.container.ContainerManager
import org.apache.log4j.Logger
import java.security.MessageDigest
import java.text.MessageFormat
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object HashTranslator {
    private var mailsByUser = mutableMapOf<String, String>()
    private var usersByHash = mutableMapOf<String, String>()

    private val LOG = Logger.getLogger(this.javaClass)
    private val lock = ReentrantLock()

    private fun String.toEmailHash() = toLowerCase().trim { it <= ' ' }.toMD5()
    private fun String.toMD5() = MessageDigest.getInstance("MD5").digest(this.toByteArray()).toHex()
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    fun getUsername(emailhash: String): String {
        if(!usersByHash.contains(emailhash)){
            lock.withLock {
                if(!usersByHash.contains(emailhash)){
                    populateTranslation()
                }
            }
        }

        return (usersByHash[emailhash] ?: "anonymous").also {
            LOG.debug("The username found for the requested email hash ($emailhash) is $it")
        }
    }

    private fun populateTranslation() {
            val userAccessor = ContainerManager.getComponent("userAccessor") as UserAccessor
            for (user in userAccessor.users) {
                if (!user.email.isNullOrBlank() && !user.name.isNullOrBlank()) {
                    if (!userAccessor.getGroupNames(user).contains("jira-users")) {
                        LOG.debug("Skipping user [${user.name}], because group 'jira-users' is missing")
                        continue
                    }
                    if (mailsByUser[user.name] == user.email) {
                        LOG.debug("Skipping user [${user.name}], because email hasn't changed")
                        continue
                    }
                    with(user.email.toEmailHash()){
                        LOG.debug("The email hash for ${user.name} is $this")
                        mailsByUser[user.name] = user.email
                        usersByHash[this] = user.name
                    }
                }
            }
    }
}
