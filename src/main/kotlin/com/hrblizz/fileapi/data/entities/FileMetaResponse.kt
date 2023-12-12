package com.hrblizz.fileapi.data.entities

import java.time.LocalDateTime

data class FileMetaResponse(

    val token: String,
    val name: String,
    val fileContentType: String,
    val size: Int,
    val meta: Map<String, Any>,
    val source: String,
    val creationDate: LocalDateTime
)
