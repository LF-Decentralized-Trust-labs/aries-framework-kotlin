package org.hyperledger.ariesframework.credentials.v2.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hyperledger.ariesframework.agent.AgentMessage
import org.hyperledger.ariesframework.agent.decorators.Attachment
import org.hyperledger.ariesframework.credentials.CredentialsConstants
import org.hyperledger.ariesframework.credentials.v2.models.CredentialPreviewV2
import org.hyperledger.ariesframework.credentials.v2.models.Format

@Serializable
class ProposeCredentialMessageV2(
    val formats: List<Format>,

    @SerialName("filters~attach")
    val proposalAttachments: List<Attachment>,

    @SerialName("credential_preview")
    val credentialPreview: CredentialPreviewV2? = null,

    @SerialName("goal_code")
    val goalCode: String? = null,

    val goal: String? = null,

    val comment: String? = null,
) : AgentMessage(generateId(), type) {

    companion object {
        val type = CredentialsConstants.PROPOSE_CREDENTIAL_V2
    }

    fun getProposalAttachmentById(id: String): Attachment? {
        return proposalAttachments.find { it.id == id }
    }

    /**
     * Builder class for ProposeCredentialMessageV2
     */
    class Builder {
        private var formats: List<Format> = emptyList()
        private var proposalAttachments: List<Attachment> = emptyList()
        private var credentialPreview: CredentialPreviewV2? = null
        private var goalCode: String? = null
        private var goal: String? = null
        private var comment: String? = null

        fun formats(formats: List<Format>) = apply { this.formats = formats }
        fun proposalAttachments(proposalAttachments: List<Attachment>) = apply { this.proposalAttachments = proposalAttachments }
        fun credentialPreview(credentialPreview: CredentialPreviewV2?) = apply { this.credentialPreview = credentialPreview }
        fun goalCode(goalCode: String?) = apply { this.goalCode = goalCode }
        fun goal(goal: String?) = apply { this.goal = goal }
        fun comment(comment: String?) = apply { this.comment = comment }

        fun build(): ProposeCredentialMessageV2 {
            return ProposeCredentialMessageV2(
                formats = formats,
                proposalAttachments = proposalAttachments,
                credentialPreview = credentialPreview,
                goalCode = goalCode,
                goal = goal,
                comment = comment,
            )
        }
    }
}
