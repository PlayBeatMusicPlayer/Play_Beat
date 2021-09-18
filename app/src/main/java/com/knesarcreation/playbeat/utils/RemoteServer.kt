package com.knesarcreation.playbeat.utils

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.ResponseException
import java.io.IOException

class RemoteServer : NanoHTTPD(9090) {

    /*override fun serve(session: IHTTPSession): Response {
       *//* return newFixedLengthResponse("Hello Kaunain")*//*
        if (session.method == Method.GET) {
            val itemIdRequestParameter = session.parameters["itemId"]!![0]
            return newFixedLengthResponse("Requested itemId = $itemIdRequestParameter")
        }
        return newFixedLengthResponse(
            Response.Status.NOT_FOUND, MIME_PLAINTEXT,
            "The requested resource does not exist"
        )
    }*/
    override fun serve(session: IHTTPSession): Response? {
      /*  if (session.method == Method.POST) {
            try {
                session.parseBody(HashMap())
                val requestBody = session.queryParameterString
                return newFixedLengthResponse("Request body = $requestBody")
            } catch (e: IOException) {
                // handle
            } catch (e: ResponseException) {
            }
        }
        return newFixedLengthResponse(
            Response.Status.NOT_FOUND, MIME_PLAINTEXT,
            "The requested resource does not exist"
        )*/
        val response = newFixedLengthResponse("Hello world")
        response.addHeader("Access-Control-Allow-Origin", "*")
        return response
    }

    companion object {
        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            RemoteServer()
        }
    }

    init {
        start(SOCKET_READ_TIMEOUT, false)
    }
}