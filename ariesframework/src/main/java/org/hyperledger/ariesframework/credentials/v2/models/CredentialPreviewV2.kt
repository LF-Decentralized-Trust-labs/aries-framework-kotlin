package org.hyperledger.ariesframework.credentials.v2.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hyperledger.ariesframework.credentials.CredentialsConstants
import org.hyperledger.ariesframework.credentials.models.CredentialPreviewAttribute

@Serializable
class CredentialPreviewV2(
    var attributes: List<CredentialPreviewAttribute>,
) {
    @SerialName("@type")
    val type: String = CredentialsConstants.CREDENTIAL_PREVIEW_V2

    constructor(options: CredentialPreviewOptions) : this(
        attributes = options.attributes.map {
            CredentialPreviewAttribute(it)
        },
    )

    fun toJSON(): String {
        return Json.encodeToString(this)
    }

    companion object {
        fun fromDictionary(record: Map<String, String>): CredentialPreviewV2 {
            val attributes = record.map { (name, value) ->
                CredentialPreviewAttribute(name, "text/plain", value)
            }
            return CredentialPreviewV2(attributes)
        }
    }
}
