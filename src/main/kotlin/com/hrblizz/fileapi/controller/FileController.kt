package com.hrblizz.fileapi.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hrblizz.fileapi.data.entities.FileMetaData
import com.hrblizz.fileapi.data.entities.FileMetaResponse
import com.hrblizz.fileapi.library.log.ExceptionLogItem
import com.hrblizz.fileapi.library.log.Logger
import com.hrblizz.fileapi.service.FileService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.DeferredResult
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/files")
class FileController(
    private val fileService: FileService,
    private val logger: Logger) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestParam("name") name: String,
        @RequestParam("fileContentType") fileContentType: String,
        @RequestParam("meta") meta: String,
        @RequestParam("source") source: String,
        @RequestParam("expireTime") expireTime: String?,
        @RequestParam("content") content: MultipartFile
    ): DeferredResult<ResponseEntity<out Map<String, Any>>> {
        val deferredResult = DeferredResult<ResponseEntity<out Map<String, Any>>>()

        try {
            CompletableFuture.supplyAsync {
                val errorResponse = fileService.validateFields(name, fileContentType, meta, source, content)
                if (errorResponse.isNotEmpty()) {
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
                } else {
                    val objectMapper = jacksonObjectMapper()
                    val metaMap: Map<String, Any> = objectMapper.readValue(
                        meta, object : TypeReference<Map<String, Any>>() {}
                    )

                    val token = fileService.uploadFile(
                        name, fileContentType, metaMap, source, expireTime, content.bytes
                    )
                    val response = mapOf("token" to token)
                    ResponseEntity.status(HttpStatus.CREATED).body(response)
                }
            }.thenAccept { result ->
                deferredResult.setResult(result)
            }
        } catch (e: Exception) {
            logger.crit(ExceptionLogItem("An error occurred during file upload", e))
            deferredResult.setErrorResult(HttpStatus.SERVICE_UNAVAILABLE)
        }

        return deferredResult
    }

    @PostMapping("/metas", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getFileMetadata(@RequestBody request: RequestBodyClass
    ): ResponseEntity<Map<String, Map<String, FileMetaResponse>>> {
        try {
            val filesMeta = fileService.getFilesMetaData(request.tokens)
            return ResponseEntity(filesMeta, HttpStatus.OK)
        } catch (e: Exception) {
            logger.crit(ExceptionLogItem("An error occurred during getting files metas", e))
        }
        return ResponseEntity(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @GetMapping("/{token}")
    fun getFileByToken(@PathVariable token: String): Any? {
        try {
            val fileMetaData = fileService.getFileByToken(token)

            if (fileMetaData != null) {
                val headers = HttpHeaders()
                headers.add("X-Filename", fileMetaData.name)
                headers.add("X-Filesize", fileMetaData.content.size.toString())
                headers.add("X-CreateTime", fileMetaData.creationDate.toString())
                headers.add("Content-Type", fileMetaData.fileContentType)

                return ResponseEntity(fileMetaData.content, headers, HttpStatus.OK.value())
            }

            return ResponseEntity("File with token $token does not exist", HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            logger.crit(ExceptionLogItem("An error occurred during getting file by token : $token", e))
        }
        return ResponseEntity("", HttpStatus.SERVICE_UNAVAILABLE)
    }

    @DeleteMapping("/{token}")
    fun deleteFile(@PathVariable token: String): ResponseEntity<String> {
        try {
            val fileMetaData = fileService.getFileByToken(token)

            if (fileMetaData != null) {
                fileService.deleteFile(token)
                return ResponseEntity("File with token $token has been deleted", HttpStatus.OK)
            }

            return ResponseEntity("File with token $token does not exist", HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            logger.crit(ExceptionLogItem("An error occurred during deleting file with token : $token ", e))
            return ResponseEntity(HttpStatus.SERVICE_UNAVAILABLE)
        }
    }

    @GetMapping("/all")
    fun getAllFiles(): ResponseEntity<List<FileMetaData>> {
        try {
            val files = fileService.getAllFiles()
            return ResponseEntity(files, HttpStatus.OK)
        } catch (e: Exception) {
            logger.crit(ExceptionLogItem("An error occurred during getting ALL files", e))
        }
        return ResponseEntity(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @DeleteMapping("/deleteAll")
    fun deleteAll(): ResponseEntity<String> {
        try {
            fileService.deleteAll()
            return ResponseEntity(HttpStatus.OK)
        } catch (e: Exception) {
            logger.crit(ExceptionLogItem("An error occurred during deleting all files", e))
        }
        return ResponseEntity(HttpStatus.SERVICE_UNAVAILABLE)
    }

    data class RequestBodyClass(val tokens: List<String>)
}