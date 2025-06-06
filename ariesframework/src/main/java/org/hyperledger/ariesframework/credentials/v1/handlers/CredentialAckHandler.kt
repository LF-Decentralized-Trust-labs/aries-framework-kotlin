package org.hyperledger.ariesframework.credentials.v1.handlers

import org.hyperledger.ariesframework.InboundMessageContext
import org.hyperledger.ariesframework.OutboundMessage
import org.hyperledger.ariesframework.agent.Agent
import org.hyperledger.ariesframework.agent.MessageHandler
import org.hyperledger.ariesframework.credentials.v1.messages.CredentialAckMessage
import org.slf4j.LoggerFactory

class CredentialAckHandler(val agent: Agent) : MessageHandler {
    private val logger = LoggerFactory.getLogger(CredentialAckHandler::class.java)
    override val messageType = CredentialAckMessage.type

    override suspend fun handle(messageContext: InboundMessageContext): OutboundMessage? {
        agent.credentialService.processAck(messageContext)
        return null
    }
}
