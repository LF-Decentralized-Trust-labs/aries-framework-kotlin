package org.hyperledger.ariesframework.credentials.v2.handlers

import org.hyperledger.ariesframework.InboundMessageContext
import org.hyperledger.ariesframework.OutboundMessage
import org.hyperledger.ariesframework.agent.Agent
import org.hyperledger.ariesframework.agent.MessageHandler
import org.hyperledger.ariesframework.credentials.v2.messages.CredentialAckMessageV2
import org.slf4j.LoggerFactory

class CredentialAckHandlerV2(val agent: Agent) : MessageHandler {

    private val logger = LoggerFactory.getLogger(CredentialAckHandlerV2::class.java)
    override val messageType = CredentialAckMessageV2.type

    override suspend fun handle(messageContext: InboundMessageContext): OutboundMessage? {
        logger.debug("CredentialAckHandlerV2 init")
        agent.credentialServiceV2.processAck(messageContext)
        return null
    }
}
