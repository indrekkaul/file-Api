package com.hrblizz.fileapi.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hrblizz.fileapi.data.entities.FileMetaData
import com.hrblizz.fileapi.data.entities.FileMetaResponse
import com.hrblizz.fileapi.data.repository.FileRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class FileService(@Autowired val fileRepository: FileRepository) {

    fun uploadFile(
        name: String,
        contentType: String,
        meta: Map<String, Any>,
        source: String,
        expireTime: String?,
        content: ByteArray
    ): String {
        val token = generateToken()

        val fileMetaDataEntity = FileMetaData()
        fileMetaDataEntity.token = token
        fileMetaDataEntity.name = name
        fileMetaDataEntity.fileContentType = contentType
        fileMetaDataEntity.meta = meta
        fileMetaDataEntity.source = source
        fileMetaDataEntity.expireTime = expireTime
        fileMetaDataEntity.content = content + "\n".toByteArray()

        fileRepository.save(fileMetaDataEntity)

        return token
    }

    fun getFileByToken(token: String): FileMetaData? {
        return fileRepository.findById(token).orElse(null)
    }

    fun getFilesMetaData(tokens: List<String>): Map<String, Map<String, FileMetaResponse>> {
        val responseMap: MutableMap<String, Map<String, FileMetaResponse>> = mutableMapOf()
        val filesMap: MutableMap<String, FileMetaResponse> = mutableMapOf()

        for (token in tokens) {
            val fileMetaData = fileRepository.findById(token).orElse(null)

            if (fileMetaData != null) {
                val fileMetaResponse = convertToResponse(fileMetaData)
                filesMap[token] = fileMetaResponse
            }
        }

        responseMap["files"] = filesMap

        return responseMap
    }

    fun convertToResponse(fileMetaData: FileMetaData): FileMetaResponse {
        return FileMetaResponse(
            token = fileMetaData.token,
            name = fileMetaData.name,
            fileContentType = fileMetaData.fileContentType,
            size = fileMetaData.content.size,
            meta = fileMetaData.meta,
            source = fileMetaData.source,
            creationDate = fileMetaData.creationDate
        )
    }

    fun validateFields(name: String?, fileContentType: String?, meta: String, source: String?, content: MultipartFile)
    : Map<String, Any> {
        val validationErrors = mutableListOf<String>()

        if (name.isNullOrBlank()) {
            validationErrors.add("Name cannot be empty or null")
        }
        if (fileContentType.isNullOrBlank()) {
            validationErrors.add("File content type cannot be empty or null")
        }
        if (source.isNullOrBlank()) {
            validationErrors.add("Source cannot be empty or null")
        }
        if (content.isEmpty) {
            validationErrors.add("File is missing")
        }
        if (content.contentType != fileContentType) {
            validationErrors.add(
                "File content type mismatch. Actual ${content.contentType}, but provided $fileContentType")
        }
        try {
            jacksonObjectMapper().readTree(meta)
        } catch (e: Exception) {
            validationErrors.add("Meta is not in JSON format")
        }
        if (validationErrors.isNotEmpty()) {
            return mapOf(
                "message" to "Validation failed",
                "errors" to validationErrors
            )
        }
        return emptyMap()
    }

    fun deleteFile(token: String) {
        fileRepository.deleteById(token)
    }

    fun generateToken(): String {
        return UUID.randomUUID().toString()
    }

    fun getAllFiles(): List<FileMetaData> {
        return fileRepository.findAll()
    }

    fun deleteAll() {
        fileRepository.deleteAll()
    }
}