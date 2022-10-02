package org.emulinker.kaillera.lookingforgame

import org.emulinker.kaillera.model.KailleraUser

data class LookingForGameEvent
    constructor(val gameId: Int, val gameTitle: String, val user: KailleraUser)
