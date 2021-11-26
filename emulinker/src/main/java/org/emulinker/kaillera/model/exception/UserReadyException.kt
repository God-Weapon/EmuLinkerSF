package org.emulinker.kaillera.model.exception

import java.lang.Exception

class UserReadyException(message: String?, source: Exception? = null) :
    ActionException(message, source)
