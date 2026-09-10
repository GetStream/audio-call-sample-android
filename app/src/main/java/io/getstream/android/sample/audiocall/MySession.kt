package io.getstream.android.sample.audiocall

class MySession(val id:Int, val name: String, val token:String)
{
    fun login(){
        println("User with id: $id, name:$name and token:$token is logged in")
    }

    fun logout(){
        println("User with id: $id, name:$name and token:$token logged out")
    }
}