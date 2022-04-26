package com.knesarcreation.playbeat.equailizer

import java.io.Serializable

class EqualizerModel : Serializable {
    var isEqualizerEnabled = false
    var isEqualizerReloaded = false
    var seekbarpos = IntArray(5)
    var presetPos = 0
    var reverbPreset: Short = -1
    var bassStrength: Short = -1

}