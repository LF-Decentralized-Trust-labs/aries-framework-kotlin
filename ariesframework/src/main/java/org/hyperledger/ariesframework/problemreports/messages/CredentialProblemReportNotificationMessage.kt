package org.hyperledger.ariesframework.problemreports.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hyperledger.ariesframework.agent.AgentMessage

@Serializable
class CredentialProblemReportNotificationMessage : AgentMessage(generateId(), type) {

    @SerialName("description")
    val description: String? = null

    companion object {
        const val type = "https://didcomm.org/notification/1.0/problem-report"
    }
}
