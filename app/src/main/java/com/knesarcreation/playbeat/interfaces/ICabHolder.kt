package com.knesarcreation.playbeat.interfaces

import com.afollestad.materialcab.attached.AttachedCab

interface ICabHolder {

    fun openCab(menuRes: Int, callback: ICabCallback): AttachedCab
}
