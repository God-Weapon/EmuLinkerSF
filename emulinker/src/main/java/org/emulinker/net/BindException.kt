package org.emulinker.net

import java.lang.Exception

class BindException(msg: String?, val port: Int, e: Exception?) : Exception(msg, e)
