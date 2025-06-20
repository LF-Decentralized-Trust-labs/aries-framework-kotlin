package org.hyperledger.ariesframework.credentials.v2.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hyperledger.ariesframework.agent.AgentMessage
import org.hyperledger.ariesframework.agent.MessageSerializer
import org.hyperledger.ariesframework.agent.decorators.Attachment
import org.hyperledger.ariesframework.credentials.CredentialsConstants
import org.hyperledger.ariesframework.credentials.v2.models.Format

@Serializable
class IssueCredentialMessageV2(
    val formats: List<Format>,

    @SerialName("credentials~attach")
    val credentialAttachments: List<Attachment>,

    @SerialName("goal_code") val goalCode: String? = null,
    val goal: String? = null,

    val comment: String? = null,
) : AgentMessage(generateId(), type) {

    companion object {
        fun decode(decode: String): IssueCredentialMessageV2 {
            return MessageSerializer.decodeFromString(decode) as IssueCredentialMessageV2
        }

        const val INDY_CREDENTIAL_ATTACHMENT_ID = "indy"
        val type = CredentialsConstants.ISSUE_CREDENTIAL_V2
    }

    fun getCredentialAttachmentById(id: String): Attachment {
        val issueAttachment = credentialAttachments.find { it.id == id }
        check(issueAttachment != null) {
            "Indy attachment with id $INDY_CREDENTIAL_ATTACHMENT_ID not found in issue message"
        }
        return issueAttachment
    }
}
