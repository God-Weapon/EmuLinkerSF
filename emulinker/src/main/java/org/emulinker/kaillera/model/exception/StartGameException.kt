package org.emulinker.kaillera.model.exception

import java.lang.Exception

class StartGameException(message: String, cause: Exception? = null) :
    ActionException(message, cause)
