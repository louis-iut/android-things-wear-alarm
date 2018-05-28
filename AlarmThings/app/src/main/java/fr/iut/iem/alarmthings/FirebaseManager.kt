package fr.iut.iem.alarmthings

import com.google.firebase.database.FirebaseDatabase

class FirebaseManager {

    var database = FirebaseDatabase.getInstance()
    var myRef = database.getReference("message")

}