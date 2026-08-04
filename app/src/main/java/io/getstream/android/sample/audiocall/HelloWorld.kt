package io.getstream.android.sample.audiocall

class HelloWorld {
    private var mySession: MySession? = null
    fun initSession(): MySession {
        val localSession = MySession(1, "hello", "token")
        localSession.login()
        mySession = localSession
        localSession.logout()
        print("initSession")
        cleanup()
        return mySession!!
    }

    fun cleanup() {
        print("Hello")
        mySession = null
        print("world")
    }
}

