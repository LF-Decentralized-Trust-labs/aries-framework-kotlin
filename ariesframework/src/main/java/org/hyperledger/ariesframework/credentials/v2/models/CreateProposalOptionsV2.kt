package org.hyperledger.ariesframework.credentials.v2.models

import kotlinx.serialization.Serializable
import org.hyperledger.ariesframework.agent.decorators.Attachment
import org.hyperledger.ariesframework.connection.repository.ConnectionRecord
import org.hyperledger.ariesframework.credentials.CredentialsConstants
import org.hyperledger.ariesframework.credentials.repository.CredentialExchangeRecord
import org.hyperledger.ariesframework.credentials.models.AutoAcceptCredential

@Serializable
data class CreateProposalOptionsV2(
    val connection: ConnectionRecord,
    val autoAcceptCredential: AutoAcceptCredential? = null,
    val credentialRecord: CredentialExchangeRecord,
    val formats: List<Format>,
    val proposalAttachments: List<Attachment>,
    val comment: String? = null,
    val goal: String? = null,
    val goalCode: String? = null,
    val credentialPreview: CredentialPreviewV2? = null,
    val protocolVersion: String,
    val credentialDefinitionId: String? = null,
    val issuerDid: String? = null,
    val threadId: String,
    val parentThreadId: String? = null,
) {
    class Builder(private val connection: ConnectionRecord, private val credentialRecord: CredentialExchangeRecord) {
        private var autoAcceptCredential: AutoAcceptCredential? = null
        private var comment: String? = null
        private var goal: String? = null
        private var goalCode: String? = null
        private var credentialPreview: CredentialPreviewV2? = null
        private lateinit var protocolVersion: String
        private var credentialDefinitionId: String? = null
        private var issuerDid: String? = null
        private lateinit var threadId: String
        private var parentThreadId: String? = null
        private lateinit var formats: List<Format>
        private lateinit var proposalAttachments: List<Attachment>

        fun autoAcceptCredential(autoAcceptCredential: AutoAcceptCredential?) = apply {
            this.autoAcceptCredential = autoAcceptCredential
        }

        fun comment(comment: String?) = apply {
            this.comment = comment
        }

        fun formats(formats: List<Format>) = apply {
            this.formats = formats
        }

        fun proposalAttachments(proposalAttachments: List<Attachment>) = apply {
            this.proposalAttachments = proposalAttachments
        }

        fun threadId(threadId: String) = apply {
            this.threadId = threadId
        }

        fun parentThreadId(parentThreadId: String?) = apply {
            this.parentThreadId = parentThreadId
        }

        fun goal(goal: String?) = apply {
            this.goal = goal
        }

        fun goalCode(goalCode: String?) = apply {
            this.goalCode = goalCode
        }

        fun credentialPreview(credentialPreview: CredentialPreviewV2?) = apply {
            this.credentialPreview = credentialPreview
        }

        fun credentialDefinitionId(credentialDefinitionId: String?) = apply {
            this.credentialDefinitionId = credentialDefinitionId
        }

        fun issuerDid(issuerDid: String?) = apply {
            this.issuerDid = issuerDid
        }

        fun build(): CreateProposalOptionsV2 {
            return CreateProposalOptionsV2(
                connection = connection,
                autoAcceptCredential = autoAcceptCredential,
                credentialRecord = credentialRecord,
                comment = comment,
                goal = goal,
                goalCode = goalCode,
                credentialPreview = credentialPreview,
                credentialDefinitionId = credentialDefinitionId,
                issuerDid = issuerDid,
                threadId = threadId,
                parentThreadId = parentThreadId,
                protocolVersion = CredentialsConstants.PROTOCOL_VERSION_V2,
                proposalAttachments = proposalAttachments,
                formats = formats,
            )
        }
    }
}
