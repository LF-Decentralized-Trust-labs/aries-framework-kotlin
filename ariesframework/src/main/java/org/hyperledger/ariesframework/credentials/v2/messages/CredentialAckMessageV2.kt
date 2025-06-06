package org.hyperledger.ariesframework.credentials.v2.messages

import kotlinx.serialization.Serializable
import org.hyperledger.ariesframework.AckStatus
import org.hyperledger.ariesframework.agent.AgentMessage
import org.hyperledger.ariesframework.agent.decorators.ThreadDecorator
import org.hyperledger.ariesframework.credentials.CredentialsConstants

@Serializable
class CredentialAckMessageV2 private constructor(val status: AckStatus) : AgentMessage(generateId(), type) {
    constructor(threadId: String, status: AckStatus) : this(status) {
        thread = ThreadDecorator(threadId)
    }
    companion object {
        const val type = CredentialsConstants.ACK_V2
    }
}
