package org.hyperledger.ariesframework.credentials.v2.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Format(
    @SerialName("attach_id")
    var attachId: String? = null,

    @SerialName("format")
    var format: String,

) {

    fun constructor(options: FormatSpec) {
        this.attachId = options.attachmentId
        this.format = options.format
    }

    override fun toString(): String {
        return "Format(attachId=$attachId, format='$format')"
    }
}
