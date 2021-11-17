package com.knesarcreation.playbeat.model

data class FolderModel(var folderId: String, var folderName: String, var noOfSongs: Int) {

    constructor() : this("", "", 0)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FolderModel

        if (folderId != other.folderId) return false
        if (folderName != other.folderName) return false
        if (noOfSongs != other.noOfSongs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = folderId.hashCode()
        result = 31 * result + folderName.hashCode()
        result = 31 * result + noOfSongs
        return result
    }


}