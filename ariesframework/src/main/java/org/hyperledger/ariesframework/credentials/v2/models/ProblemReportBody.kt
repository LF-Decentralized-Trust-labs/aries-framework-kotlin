package org.hyperledger.ariesframework.credentials.v2.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProblemReportBody(
    @SerialName("code") val code: String,
    @SerialName("comment") val comment: String,
    @SerialName("args") val args: List<String>? = null,
    @SerialName("escalate_to") val escalateTo: String? = null,
)
