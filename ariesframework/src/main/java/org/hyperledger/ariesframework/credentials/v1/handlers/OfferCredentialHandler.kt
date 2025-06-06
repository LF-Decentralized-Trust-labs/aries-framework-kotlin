package org.hyperledger.ariesframework.credentials.v1.handlers

import org.hyperledger.ariesframework.InboundMessageContext
import org.hyperledger.ariesframework.OutboundMessage
import org.hyperledger.ariesframework.agent.Agent
import org.hyperledger.ariesframework.agent.MessageHandler
import org.hyperledger.ariesframework.credentials.models.AcceptOfferOptions
import org.hyperledger.ariesframework.credentials.v1.messages.OfferCredentialMessage
import org.hyperledger.ariesframework.credentials.models.AutoAcceptCredential
import org.slf4j.LoggerFactory

class OfferCredentialHandler(val agent: Agent) : MessageHandler {
    private val logger = LoggerFactory.getLogger(OfferCredentialHandler::class.java)
    override val messageType = OfferCredentialMessage.type

    override suspend fun handle(messageContext: InboundMessageContext): OutboundMessage? {
        val credentialRecord = agent.credentialService.processOffer(messageContext)

        if (credentialRecord.autoAcceptCredential == AutoAcceptCredential.Always ||
            agent.agentConfig.autoAcceptCredential == AutoAcceptCredential.Always
        ) {
            val message = agent.credentialService.createRequest(AcceptOfferOptions(credentialRecord.id))
            return OutboundMessage(message, messageContext.connection!!)
        }

        return null
    }
}
