package org.hyperledger.ariesframework.credentials.v2

import org.hyperledger.ariesframework.agent.Agent
import org.hyperledger.ariesframework.agent.Dispatcher
import org.hyperledger.ariesframework.agent.MessageSerializer
import org.hyperledger.ariesframework.credentials.v2.handlers.CredentialAckHandlerV2
import org.hyperledger.ariesframework.credentials.v2.handlers.IssueCredentialHandlerV2
import org.hyperledger.ariesframework.credentials.v2.handlers.OfferCredentialHandlerV2
import org.hyperledger.ariesframework.credentials.v2.handlers.RequestCredentialHandlerV2
import org.hyperledger.ariesframework.credentials.v2.messages.CredentialAckMessageV2
import org.hyperledger.ariesframework.credentials.v2.messages.IssueCredentialMessageV2
import org.hyperledger.ariesframework.credentials.v2.messages.OfferCredentialMessageV2
import org.hyperledger.ariesframework.credentials.v2.messages.ProposeCredentialMessageV2
import org.hyperledger.ariesframework.credentials.v2.messages.RequestCredentialMessageV2

class Registers(val agent: Agent) {

    fun initialize() {
        registerHandlers(agent.dispatcher)
        registerMessages()
    }

    private fun registerHandlers(dispatcher: Dispatcher) {
        dispatcher.registerHandler(CredentialAckHandlerV2(agent))
        dispatcher.registerHandler(IssueCredentialHandlerV2(agent))
        dispatcher.registerHandler(OfferCredentialHandlerV2(agent))
        dispatcher.registerHandler(RequestCredentialHandlerV2(agent))
    }

    private fun registerMessages() {
        MessageSerializer.registerMessage(CredentialAckMessageV2.type, CredentialAckMessageV2::class)
        MessageSerializer.registerMessage(IssueCredentialMessageV2.type, IssueCredentialMessageV2::class)
        MessageSerializer.registerMessage(OfferCredentialMessageV2.type, OfferCredentialMessageV2::class)
        MessageSerializer.registerMessage(ProposeCredentialMessageV2.type, ProposeCredentialMessageV2::class)
        MessageSerializer.registerMessage(RequestCredentialMessageV2.type, RequestCredentialMessageV2::class)
    }
}
