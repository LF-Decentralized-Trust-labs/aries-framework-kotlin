package org.hyperledger.ariesframework.credentials.v2.handlers

import org.hyperledger.ariesframework.InboundMessageContext
import org.hyperledger.ariesframework.OutboundMessage
import org.hyperledger.ariesframework.agent.Agent
import org.hyperledger.ariesframework.agent.MessageHandler
import org.hyperledger.ariesframework.credentials.models.AcceptOfferOptions
import org.hyperledger.ariesframework.credentials.models.AutoAcceptCredential
import org.hyperledger.ariesframework.credentials.v2.messages.OfferCredentialMessageV2
import org.slf4j.LoggerFactory

class OfferCredentialHandlerV2(val agent: Agent) : MessageHandler {

    private val logger = LoggerFactory.getLogger(OfferCredentialHandlerV2::class.java)
    override val messageType = OfferCredentialMessageV2.type

    override suspend fun handle(messageContext: InboundMessageContext): OutboundMessage? {
        logger.debug("OfferCredentialHandlerV2 init")
        val credentialRecord = agent.credentialServiceV2.processOfferCredentialMessage(messageContext)

        if (credentialRecord.autoAcceptCredential == AutoAcceptCredential.Always ||
            agent.agentConfig.autoAcceptCredential == AutoAcceptCredential.Always
        ) {
            val message = agent.credentialServiceV2.createRequestCredentialMessage(
                AcceptOfferOptions(credentialRecord.id),
            )
            return OutboundMessage(message, messageContext.connection!!)
        }
        return null
    }
}
