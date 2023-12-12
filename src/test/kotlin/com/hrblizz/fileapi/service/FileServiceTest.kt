package com.hrblizz.fileapi.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hrblizz.fileapi.data.entities.FileMetaData
import com.hrblizz.fileapi.data.repository.FileRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.*

class FileServiceTest {
    @InjectMocks
    lateinit var fileService: FileService

    @Mock
    lateinit var fileRepository: FileRepository
    private lateinit var name: String
    private lateinit var contentType: String
    private lateinit var meta: Map<String, String>
    private lateinit var source: String
    private lateinit var expireTime: String
    private lateinit var content: ByteArray
    // ... existing code ...

    @Test
    fun testDeleteAllFilesWhenCalledThenRepositoryDeleteAllIsCalled() {
        fileService.deleteAll()

        verify(fileRepository, times(1)).deleteAll()
    }


    @Test
    fun testGetAllFilesWhenCalledThenRepositoryFindAllIsCalled() {
        fileService.getAllFiles()

        verify(fileRepository, times(1)).findAll()
    }

    @Test
    fun testDeleteFileWhenTokenProvidedThenRepositoryMethodCalled() {
        val token = "valid-token"

        fileService.deleteFile(token)

        verify(fileRepository, times(1)).deleteById(token)
    }

    @Test
    fun testGetFilesMetaDataWhenTokensProvidedThenReturnsCorrectMetaData() {
        val tokens = listOf("token1", "token2")
        val fileMetaData1 = FileMetaData()
        fileMetaData1.token = "token1"
        fileMetaData1.name = "token1"
        fileMetaData1.meta = mapOf("key" to "value")
        fileMetaData1.source = "token1"
        fileMetaData1.fileContentType = "token1"
        fileMetaData1.creationDate = LocalDateTime.now()
        fileMetaData1.content = "file content".toByteArray()
        val fileMetaData2 = FileMetaData()
        fileMetaData2.token = "token2"
        fileMetaData2.name = "token2"
        fileMetaData2.meta = mapOf("ke2y" to "value2")
        fileMetaData2.source = "token2"
        fileMetaData2.fileContentType = "token2"
        fileMetaData2.creationDate = LocalDateTime.now()
        fileMetaData2.content = "file content2".toByteArray()

        `when`(fileRepository.findById("token1")).thenReturn(Optional.of(fileMetaData1))
        `when`(fileRepository.findById("token2")).thenReturn(Optional.of(fileMetaData2))

        val result = fileService.getFilesMetaData(tokens)

        assertTrue(result["files"]?.containsKey("token1") ?: false)
        assertTrue(result["files"]?.containsKey("token2") ?: false)
    }

    @Test
    fun testGetFilesMetaDataWhenTokenDoesNotExistThenReturnsEmptyMetaData() {
        val tokens = listOf("token1", "token2")

        `when`(fileRepository.findById("token1")).thenReturn(Optional.empty())
        `when`(fileRepository.findById("token2")).thenReturn(Optional.empty())

        val result = fileService.getFilesMetaData(tokens)

        assertFalse(result["files"]?.containsKey("token1") ?: true)
        assertFalse(result["files"]?.containsKey("token2") ?: true)
    }

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        name = "test.txt"
        contentType = "text/plain"
        meta = mapOf("key" to "value")
        source = "source"
        expireTime = ""
        content = "Test content".toByteArray()
    }

    @Test
    fun testUploadFileThenReturnValidToken() {
        val expectedToken = UUID.randomUUID().toString()

        val result = fileService.uploadFile(name, contentType, meta, source, expireTime, content)

        assertEquals(expectedToken.length, result.length)
    }

    @Test
    fun testValidateFieldsWhenInvalidInputsThenReturnErrorMessages() {
        val name = ""
        val fileContentType = ""
        val meta = "invalid json"
        val source = ""
        val content: MultipartFile = MockMultipartFile("file", ByteArray(0))

        val result = fileService.validateFields(name, fileContentType, meta, source, content)

        assertTrue(result.isNotEmpty())
        assertEquals("Validation failed", result["message"])
        assertTrue((result["errors"] as List<*>).contains("Name cannot be empty or null"))
        assertTrue((result["errors"] as List<*>).contains("File content type cannot be empty or null"))
        assertTrue((result["errors"] as List<*>).contains("Source cannot be empty or null"))
        assertTrue((result["errors"] as List<*>).contains("File is missing"))
        assertTrue((result["errors"] as List<*>).contains("Meta is not in JSON format"))
    }

    @Test
    fun testValidateFieldsWhenValidInputsThenReturnEmptyMap() {
        val name = "test"
        val fileContentType = "text/plain"
        val meta = jacksonObjectMapper().writeValueAsString(mapOf("key" to "value"))
        val source = "test source"
        val content: MultipartFile = MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "test content".toByteArray())

        val result = fileService.validateFields(name, fileContentType, meta, source, content)

        assertTrue(result.isEmpty())
    }

    @Test
    fun testGetFileByTokenWhenTokenIsValidThenReturnsCorrectFileMetaData() {
        val token = "valid-token"
        val fileMetaData = FileMetaData()
        fileMetaData.token = token

        `when`(fileRepository.findById(token)).thenReturn(Optional.of(fileMetaData))

        val result = fileService.getFileByToken(token)

        assertEquals(fileMetaData, result)
    }

    @Test
    fun testGetFileByTokenWhenTokenIsInvalidThenReturnsNull() {
        val token = "invalid-token"

        `when`(fileRepository.findById(token)).thenReturn(Optional.empty())

        val result = fileService.getFileByToken(token)

        assertNull(result)
    }
}