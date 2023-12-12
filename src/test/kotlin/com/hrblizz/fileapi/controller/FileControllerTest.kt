package com.hrblizz.fileapi.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hrblizz.fileapi.data.entities.FileMetaData
import com.hrblizz.fileapi.data.entities.FileMetaResponse
import com.hrblizz.fileapi.library.log.Logger
import com.hrblizz.fileapi.service.FileService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var fileService: FileService

    @MockBean
    private lateinit var logger: Logger

    private val objectMapper = ObjectMapper()

    private lateinit var fileController: FileController
    private lateinit var name: String
    private lateinit var fileContentType: String
    private lateinit var meta: String
    private lateinit var metaMap: Map<String, Any>
    private lateinit var source: String
    private lateinit var expireTime: String
    private lateinit var content: MockMultipartFile
    private lateinit var token: String

    @BeforeEach
    fun setUp() {
        // Authentication information
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val principal = User("testUser", "password", authorities)
        val authentication = UsernamePasswordAuthenticationToken(principal, "password", authorities)
        SecurityContextHolder.getContext().authentication = authentication

        fileController = FileController(fileService, logger)

        name = "test-file"
        fileContentType = "application/pdf"
        meta = "{\"creatorEmployeeId\": 1}"
        metaMap = objectMapper.readValue(meta, object : TypeReference<Map<String, Any>>() {})
        source = "test-source"
        expireTime = ""
        content = MockMultipartFile("file",
            "test.pdf",
            "text/plain",
            "test content".toByteArray())
        token = "test-token"
    }

    @Test
    fun testUploadFileWhenFieldsAreValidThenReturnToken() {
        val expectedToken = "generatedToken"
        `when`(fileService.validateFields(name, fileContentType, meta, source, content)).thenReturn(emptyMap())
        `when`(fileService.uploadFile(name, fileContentType, metaMap, source, expireTime, content.bytes))
            .thenReturn(CompletableFuture.completedFuture(expectedToken).toString())

        val deferredResult = fileController.uploadFile(name, fileContentType, meta, source, expireTime, content)

        val response = ResponseEntity.status(HttpStatus.CREATED).body(mapOf("token" to expectedToken))
        deferredResult.setResult(response)

        assertTrue(deferredResult.hasResult())
        val responseEntity = deferredResult.result as? ResponseEntity<*>
        assertNotNull(responseEntity)
        val responseBody = responseEntity?.body as? Map<*, *>
        val actualToken = responseBody?.get("token")

        assertEquals(expectedToken, actualToken)
    }

    @Test
    fun testUploadFileWhenFieldsAreInvalidThenReturnBadRequest() {
        val errorResponse = mapOf("message" to "Validation failed", "errors" to listOf("Name cannot be empty or null"))

        `when`(fileService.validateFields(name, fileContentType, meta, source, content)).thenReturn(errorResponse)

        // Verify the response code 400
        mockMvc.perform(multipart("/files")
            .file(content)
            .param("name", name)
            .param("fileContentType", fileContentType)
            .param("meta", meta)
            .param("source", source)
            .param("expireTime", expireTime)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun testGetFileMetadataWhenSuccess() {
        val tokens = listOf("token1", "token2")
        val request = FileController.RequestBodyClass(tokens)
        val expectedResponse = mapOf("files" to mapOf("token1" to FileMetaResponse(
            token = "token1",
            name = "name1",
            fileContentType = "type1",
            size = 100,
            meta = mapOf("key1" to "value1"),
            source = "source1",
            creationDate = LocalDateTime.now()
        )))

        `when`(fileService.getFilesMetaData(tokens)).thenReturn(expectedResponse)

        val response = fileController.getFileMetadata(request)

        assertEquals(expectedResponse, response.body)
        assertEquals(200, response.statusCodeValue)
    }

    @Test
    fun testGetFileMetadataWhenExceptionThrown() {
        val tokens = listOf("token1", "token2")
        val request = FileController.RequestBodyClass(tokens)

        `when`(fileService.getFilesMetaData(tokens)).thenThrow(RuntimeException::class.java)

        val response = fileController.getFileMetadata(request)

        assertEquals(503, response.statusCodeValue)
    }

    @Test
    fun testGetFileByTokenWhenFileExistsThenReturnResponseEntity() {
        val fileMetaData = FileMetaData()
        fileMetaData.name = "test-file"
        fileMetaData.content = "file-content".toByteArray()
        fileMetaData.fileContentType = "application/pdf"

        `when`(fileService.getFileByToken(token)).thenReturn(fileMetaData)

        val response = fileController.getFileByToken(token)

        assert(response is ResponseEntity<*>)
        val responseEntity = response as ResponseEntity<*>
        assert(responseEntity.statusCode == HttpStatus.OK)
        assert(responseEntity.body == fileMetaData.content)

        val headers = responseEntity.headers
        assert(headers.getFirst("X-Filename") == fileMetaData.name)
        assert(headers.getFirst("X-Filesize") == fileMetaData.content.size.toString())
        assert(headers.getFirst("X-CreateTime") == fileMetaData.creationDate.toString())
        assert(headers.getFirst("Content-Type") == fileMetaData.fileContentType)
    }

    @Test
    fun testGetFileByTokenWhenFileDoesNotExistThenReturnBadRequest() {
        val token = "non-existing-token"
        `when`(fileService.getFileByToken(token)).thenReturn(null)

        val response = fileController.getFileByToken(token)
        val responseEntity = response as ResponseEntity<*>

        assert(responseEntity.statusCode == HttpStatus.BAD_REQUEST)
        assert(responseEntity.body == "File with token $token does not exist")
    }

    @Test
    fun testGetFileByTokenWhenExceptionThrownThenReturnServiceUnavailable() {
        `when`(fileService.getFileByToken(token)).thenThrow(RuntimeException::class.java)

        val response = fileController.getFileByToken(token)
        val responseEntity = response as ResponseEntity<*>

        assert(responseEntity.statusCode == HttpStatus.SERVICE_UNAVAILABLE)
        assert(responseEntity.body == "")
    }

    @Test
    fun testDeleteFileWhenTokenIsValidThenSuccess() {
        val fileMetaData = FileMetaData()
        fileMetaData.token = token

        `when`(fileService.getFileByToken(token)).thenReturn(fileMetaData)

        val response = fileController.deleteFile(token)
        val responseEntity = response as ResponseEntity<*>

        assert(responseEntity.statusCode == HttpStatus.OK)
        assert(responseEntity.body == "File with token $token has been deleted")
    }

    @Test
    fun testDeleteFileWhenTokenIsInvalidThenFailure() {
        val invalidToken = "invalid-token"
        `when`(fileService.getFileByToken(invalidToken)).thenReturn(null)

        val response = fileController.deleteFile(invalidToken)
        val responseEntity = response as ResponseEntity<*>

        assert(responseEntity.statusCode == HttpStatus.BAD_REQUEST)
        assert(responseEntity.body == "File with token $invalidToken does not exist")
    }

    @Test
    fun testDeleteFileWhenExceptionThrownThenServiceUnavailable() {
        `when`(fileService.getFileByToken(token)).thenThrow(RuntimeException::class.java)

        val response = fileController.deleteFile(token)
        val responseEntity = response as ResponseEntity<*>

        assert(responseEntity.statusCode == HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test
    fun testGetAllFilesWhenCalledThenReturnsCorrectResponse() {
        val expectedFiles = listOf(FileMetaData(), FileMetaData())
        `when`(fileService.getAllFiles()).thenReturn(expectedFiles)

        val response = fileController.getAllFiles()
        val responseEntity = response as ResponseEntity<*>

        assert(responseEntity.statusCode == HttpStatus.OK)
        assert(responseEntity.body == expectedFiles)
    }

    @Test
    fun testGetAllFilesWhenExceptionThrownThenReturnsServiceUnavailable() {
        `when`(fileService.getAllFiles()).thenThrow(RuntimeException::class.java)

        val response = fileController.getAllFiles()
        val responseEntity = response as ResponseEntity<*>

        assert(responseEntity.statusCode == HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test
    fun testDeleteAllWhenEndpointHitThenServiceMethodCalled() {
        mockMvc.perform(MockMvcRequestBuilders.delete("/files/deleteAll"))
            .andExpect(status().isOk)

        verify(fileService, Mockito.times(1)).deleteAll()
    }

    @Test
    fun testDeleteAllWhenExceptionThrownThenLoggerCritCalled() {
        `when`(fileService.deleteAll()).thenThrow(RuntimeException::class.java)

        val response = fileController.deleteAll()
        val responseEntity = response as ResponseEntity<*>

        assert(responseEntity.statusCode == HttpStatus.SERVICE_UNAVAILABLE)
    }
}