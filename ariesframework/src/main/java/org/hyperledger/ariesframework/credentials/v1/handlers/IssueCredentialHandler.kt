package org.hyperledger.ariesframework.credentials.v1.handlers

import org.hyperledger.ariesframework.InboundMessageContext
import org.hyperledger.ariesframework.OutboundMessage
import org.hyperledger.ariesframework.agent.Agent
import org.hyperledger.ariesframework.agent.MessageHandler
import org.hyperledger.ariesframework.credentials.models.AcceptCredentialOptions
import org.hyperledger.ariesframework.credentials.v1.messages.IssueCredentialMessage
import org.hyperledger.ariesframework.credentials.models.AutoAcceptCredential
import org.slf4j.LoggerFactory

class IssueCredentialHandler(val agent: Agent) : MessageHandler {
    private val logger = LoggerFactory.getLogger(IssueCredentialHandler::class.java)
    override val messageType = IssueCredentialMessage.type

    override suspend fun handle(messageContext: InboundMessageContext): OutboundMessage? {
        val credentialRecord = agent.credentialService.processCredential(messageContext)

        if (credentialRecord.autoAcceptCredential == AutoAcceptCredential.Always ||
            agent.agentConfig.autoAcceptCredential == AutoAcceptCredential.Always
        ) {
            val message = agent.credentialService.createAck(AcceptCredentialOptions(credentialRecord.id))
            return OutboundMessage(message, messageContext.connection!!)
        }

        return null
    }
}
