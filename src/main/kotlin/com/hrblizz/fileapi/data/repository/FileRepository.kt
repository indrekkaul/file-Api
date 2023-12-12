package com.hrblizz.fileapi.data.repository

import com.hrblizz.fileapi.data.entities.FileMetaData
import org.springframework.data.mongodb.repository.MongoRepository

interface FileRepository : MongoRepository<FileMetaData, String> {
}