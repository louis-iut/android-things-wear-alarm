package fr.iut.iem.alarmwear

import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference





class MainActivity : WearableActivity() {

    var database = FirebaseDatabase.getInstance()
    var myRef = database.getReference("message")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Enables Always-on
        setAmbientEnabled()
    }
}
