package org.hyperledger.ariesframework.credentials.v1.messages

import kotlinx.serialization.Serializable
import org.hyperledger.ariesframework.AckStatus
import org.hyperledger.ariesframework.agent.AgentMessage
import org.hyperledger.ariesframework.agent.decorators.ThreadDecorator
import org.hyperledger.ariesframework.credentials.CredentialsConstants

@Serializable
class CredentialAckMessage private constructor(
    val status: AckStatus,
) : AgentMessage(generateId(), type) {
    constructor(threadId: String, status: AckStatus) : this(status) {
        thread = ThreadDecorator(threadId)
    }
    companion object {
        const val type = CredentialsConstants.ACK_V1
    }

    override fun requestResponse(): Boolean {
        return false
    }
}
