package com.hrblizz.fileapi.library.log

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.PrintWriter
import java.io.StringWriter

open class ExceptionLogItem constructor(
    message: String,
    @JsonIgnore val throwable: Throwable
) : LogItem("$message: <${throwable::class.java.name}>") {
    val stacktrace: String

    init {
        this.type = "exception"

        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))
        this.stacktrace = stringWriter.toString()
    }

    override fun toString(): String {
        return "[$dateTime] [$correlationId] $message \n $stacktrace"
    }
}
