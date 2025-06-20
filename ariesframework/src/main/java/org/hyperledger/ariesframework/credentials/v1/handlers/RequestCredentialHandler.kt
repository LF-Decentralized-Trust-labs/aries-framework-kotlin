package org.hyperledger.ariesframework.credentials.v1.handlers

import org.hyperledger.ariesframework.InboundMessageContext
import org.hyperledger.ariesframework.OutboundMessage
import org.hyperledger.ariesframework.agent.Agent
import org.hyperledger.ariesframework.agent.MessageHandler
import org.hyperledger.ariesframework.credentials.models.AcceptRequestOptions
import org.hyperledger.ariesframework.credentials.v1.messages.RequestCredentialMessage
import org.hyperledger.ariesframework.credentials.models.AutoAcceptCredential
import org.slf4j.LoggerFactory

class RequestCredentialHandler(val agent: Agent) : MessageHandler {
    private val logger = LoggerFactory.getLogger(RequestCredentialHandler::class.java)
    override val messageType = RequestCredentialMessage.type

    override suspend fun handle(messageContext: InboundMessageContext): OutboundMessage? {
        val credentialRecord = agent.credentialService.processRequest(messageContext)

        if (credentialRecord.autoAcceptCredential == AutoAcceptCredential.Always ||
            agent.agentConfig.autoAcceptCredential == AutoAcceptCredential.Always
        ) {
            val message = agent.credentialService.createCredential(AcceptRequestOptions(credentialRecord.id))
            return OutboundMessage(message, messageContext.connection!!)
        }

        return null
    }
}
