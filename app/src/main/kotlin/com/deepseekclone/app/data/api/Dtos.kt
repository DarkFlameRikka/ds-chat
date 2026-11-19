package com.deepseekclone.app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 仅保留简单的 REST 响应结构；Messages API 的复杂多态内容块用 JSON 树直接处理（见 DeepSeekClient）。

@Serializable
internal data class ModelsResponse(val data: List<ModelDto> = emptyList())

@Serializable
internal data class ModelDto(val id: String = "", val ownedBy: String? = null)

@Serializable
internal data class BalanceResponse(
    @SerialName("is_available") val isAvailable: Boolean = false,
    @SerialName("balance_infos") val balanceInfos: List<BalanceItem> = emptyList(),
)

@Serializable
internal data class BalanceItem(
    val currency: String = "",
    @SerialName("total_balance") val totalBalance: String = "0",
    @SerialName("granted_balance") val grantedBalance: String = "0",
    @SerialName("topped_up_balance") val toppedUpBalance: String = "0",
)

@Serializable
internal data class FileObjectDto(
    val id: String = "",
    val filename: String = "",
    @SerialName("mime_type") val mimeType: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0,
)

@Serializable
internal data class AsrResponse(val text: String = "")

@Serializable
internal data class ErrorEnvelope(val error: ErrorBody? = null)

@Serializable
internal data class ErrorBody(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null,
)
