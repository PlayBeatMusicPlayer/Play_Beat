package com.knesarcreation.playbeat.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore


class GetRealPathOfUri() {

    fun getUriRealPath(ctx: Context, uri: Uri): String? {
        var ret: String? = ""
        ret = if (isAboveKitKat()) {
            // Android sdk version number bigger than 19.
            getUriRealPathAboveKitkat(ctx, uri)
        } else {
            // Android sdk version number smaller than 19.
            getImageRealPath(ctx.contentResolver, uri, null)
        }
        return ret
    }

    /*
   This method will parse out the real local file path from the file content URI.
   The method is only applied to android sdk version number that is bigger than 19.
    */
    private fun getUriRealPathAboveKitkat(ctx: Context?, uri: Uri?): String? {
        var ret: String? = ""
        if (ctx != null && uri != null) {
            if (isContentUri(uri)) {
                ret = if (isGooglePhotoDoc(uri.authority)) {
                    uri.lastPathSegment
                } else {
                    getImageRealPath(ctx.contentResolver, uri, null)
                }
            } else if (isFileUri(uri)) {
                ret = uri.path
            } else if (isDocumentUri(ctx, uri)) {
                // Get uri related document id.
                val documentId = DocumentsContract.getDocumentId(uri)
                // Get uri authority.
                val uriAuthority = uri.authority
                if (isMediaDoc(uriAuthority)) {
                    val idArr = documentId.split(":").toTypedArray()
                    if (idArr.size == 2) {
                        // First item is document type.
                        val docType = idArr[0]
                        // Second item is document real id.
                        val realDocId = idArr[1]
                        // Get content uri by document type.
                        var mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        when (docType) {
                            "image" -> {
                                mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            }
                            "video" -> {
                                mediaContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            }
                            "audio" -> {
                                mediaContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            }
                        }
                        // Get where clause with real document id.
                        val whereClause = MediaStore.Images.Media._ID + " = " + realDocId
                        ret = getImageRealPath(ctx.contentResolver, mediaContentUri, whereClause)
                    }
                } else if (isDownloadDoc(uriAuthority)) {
                    // Build download uri.
                    val downloadUri = Uri.parse("content://downloads/public_downloads")
                    // Append download document id at uri end.
                    val downloadUriAppendId =
                        ContentUris.withAppendedId(downloadUri, java.lang.Long.valueOf(documentId))
                    ret = getImageRealPath(ctx.contentResolver, downloadUriAppendId, null)
                } else if (isExternalStoreDoc(uriAuthority)) {
                    val idArr = documentId.split(":").toTypedArray()
                    if (idArr.size == 2) {
                        val type = idArr[0]
                        val realDocId = idArr[1]
                        if ("primary".equals(type, ignoreCase = true)) {
                            ret = Environment.getExternalStorageDirectory()
                                .toString() + "/" + realDocId
                        }
                    }
                }
            }
        }
        return ret
    }

    /* Check whether current android os version is bigger than kitkat or not. */
    private fun isAboveKitKat(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
    }

    /* Check whether this uri represent a document or not. */
    private fun isDocumentUri(ctx: Context?, uri: Uri?): Boolean {
        var ret = false
        if (ctx != null && uri != null) {
            ret = DocumentsContract.isDocumentUri(ctx, uri)
        }
        return ret
    }

    /* Check whether this uri is a content uri or not.
    *  content uri like content://media/external/images/media/1302716
    */
    private fun isContentUri(uri: Uri?): Boolean {
        var ret = false
        if (uri != null) {
            val uriSchema = uri.scheme
            if ("content".equals(uriSchema, ignoreCase = true)) {
                ret = true
            }
        }
        return ret
    }

    /* Check whether this uri is a file uri or not.
     *  file uri like file:///storage/41B7-12F1/DCIM/Camera/IMG_20180211_095139.jpg
    */
    private fun isFileUri(uri: Uri?): Boolean {
        var ret = false
        if (uri != null) {
            val uriSchema = uri.scheme
            if ("file".equals(uriSchema, ignoreCase = true)) {
                ret = true
            }
        }
        return ret
    }

    /* Check whether this document is provided by ExternalStorageProvider. Return true means the file is saved in external storage. */
    private fun isExternalStoreDoc(uriAuthority: String?): Boolean {
        var ret = false
        if ("com.android.externalstorage.documents" == uriAuthority) {
            ret = true
        }
        return ret
    }

    /* Check whether this document is provided by DownloadsProvider. return true means this file is a downloaed file. */
    private fun isDownloadDoc(uriAuthority: String?): Boolean {
        var ret = false
        if ("com.android.providers.downloads.documents" == uriAuthority) {
            ret = true
        }
        return ret
    }

    /*
    Check if MediaProvider provide this document, if true means this image is created in android media app.
    */
    private fun isMediaDoc(uriAuthority: String?): Boolean {
        var ret = false
        if ("com.android.providers.media.documents" == uriAuthority) {
            ret = true
        }
        return ret
    }

    /*
    Check whether google photos provide this document, if true means this image is created in google photos app.
    */
    private fun isGooglePhotoDoc(uriAuthority: String?): Boolean {
        var ret = false
        if ("com.google.android.apps.photos.content" == uriAuthority) {
            ret = true
        }
        return ret
    }

    /* Return uri represented document file real local path.*/
    @SuppressLint("Recycle")
    private fun getImageRealPath(
        contentResolver: ContentResolver,
        uri: Uri,
        whereClause: String?
    ): String {
        var ret = ""
        // Query the uri with condition.
        val cursor: Cursor? = contentResolver.query(uri, null, whereClause, null, null)
        if (cursor != null) {
            val moveToFirst: Boolean = cursor.moveToFirst()
            if (moveToFirst) {
                // Get columns name by uri type.
                var columnName = MediaStore.Images.Media.DATA
                when {
                    uri === MediaStore.Images.Media.EXTERNAL_CONTENT_URI -> {
                        columnName = MediaStore.Images.Media.DATA
                    }
                    uri === MediaStore.Audio.Media.EXTERNAL_CONTENT_URI -> {
                        columnName = MediaStore.Audio.Media.DATA
                    }
                    uri === MediaStore.Video.Media.EXTERNAL_CONTENT_URI -> {
                        columnName = MediaStore.Video.Media.DATA
                    }
                }
                // Get column index.
                val imageColumnIndex: Int = cursor.getColumnIndex(columnName)
                // Get column value which is the uri related file local path.
                ret = cursor.getString(imageColumnIndex)
            }
        }
        return ret
    }
}