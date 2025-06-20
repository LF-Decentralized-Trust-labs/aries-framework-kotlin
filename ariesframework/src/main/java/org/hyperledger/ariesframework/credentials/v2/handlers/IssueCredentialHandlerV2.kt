package org.hyperledger.ariesframework.credentials.v2.handlers

import org.hyperledger.ariesframework.InboundMessageContext
import org.hyperledger.ariesframework.OutboundMessage
import org.hyperledger.ariesframework.agent.Agent
import org.hyperledger.ariesframework.agent.MessageHandler
import org.hyperledger.ariesframework.credentials.models.AcceptCredentialOptions
import org.hyperledger.ariesframework.credentials.models.AutoAcceptCredential
import org.hyperledger.ariesframework.credentials.v2.messages.IssueCredentialMessageV2
import org.slf4j.LoggerFactory

class IssueCredentialHandlerV2(val agent: Agent) : MessageHandler {

    private val logger = LoggerFactory.getLogger(IssueCredentialHandlerV2::class.java)
    override val messageType = IssueCredentialMessageV2.type

    override suspend fun handle(messageContext: InboundMessageContext): OutboundMessage? {
        logger.debug("IssueCredentialHandlerV2 init")
        val credentialRecord = agent.credentialServiceV2.processIssueCredentialMessage(messageContext)

        if (credentialRecord.autoAcceptCredential == AutoAcceptCredential.Always ||
            agent.agentConfig.autoAcceptCredential == AutoAcceptCredential.Always
        ) {
            val message = agent.credentialServiceV2.createCredentialAckMessage(
                AcceptCredentialOptions(credentialRecord.id),
            )
            return OutboundMessage(message, messageContext.connection!!)
        }
        return null
    }
}
