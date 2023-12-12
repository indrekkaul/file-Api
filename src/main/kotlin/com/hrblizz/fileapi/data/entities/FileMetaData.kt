package com.hrblizz.fileapi.data.entities

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "files")
class FileMetaData {
    @Id
    lateinit var token: String

    lateinit var name: String
    lateinit var fileContentType: String
    lateinit var meta: Map<String, Any>
    lateinit var source: String
    var expireTime: String? = null
    lateinit var content: ByteArray

    @CreatedDate
    var creationDate: LocalDateTime = LocalDateTime.now()
}