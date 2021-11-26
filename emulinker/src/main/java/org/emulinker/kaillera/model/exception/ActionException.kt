package org.emulinker.kaillera.model.exception

open class ActionException(message: String? = null, source: Exception? = null) :
    Exception(message, source)
