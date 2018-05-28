package fr.iut.iem.alarmwear

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.Visibility
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : WearableActivity() {

    // database references
    var database = FirebaseDatabase.getInstance()
    var myRef = database.getReference("")
    var imageNameRef = myRef.child("imageName")
    var attackRef = myRef.child("attack")
    var activatedRef = myRef.child("activated")

    //alarm is activated
    var alarm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //set activated alarm in firebase to false
        activatedRef.setValue(false)

        //if alarm is not activated show just button to activate it
        if (alarm == false){
            attack_button.visibility = View.GONE
            done_button.visibility = View.GONE
            photo_from_firebase.visibility = View.GONE
        }

        imageNameRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {//when the image on database change
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = dataSnapshot.getValue(String::class.java)
                if (value != null) {
                    if (alarm == true){//show image and buttons
                        photo_from_firebase.visibility = View.VISIBLE
                        attack_button.visibility = View.VISIBLE
                        done_button.visibility = View.VISIBLE
                        activate_alarm_button.visibility = View.GONE
                    }
                    stringToBitMap(value)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w("onCancelled", "Failed to read value.", error.toException())
            }
        })

        //set attack in firebase to true
        attack_button.setOnClickListener {
            attackRef.setValue(true)
            Toast.makeText(this, "attack started", Toast.LENGTH_SHORT).show()
        }

        //set attack to false
        done_button.setOnClickListener {
            attackRef.setValue(false)
            Toast.makeText(this, "it's Ok", Toast.LENGTH_SHORT).show()
            imageNameRef.setValue("")
            attack_button.visibility = View.GONE
            done_button.visibility = View.GONE
            activate_alarm_button.visibility = View.VISIBLE
        }

        //set activated to true
        activate_alarm_button.setOnClickListener {
            attack_button.visibility = View.GONE
            done_button.visibility = View.GONE
            //activate_alarm_button.visibility = View.GONE
            if (alarm == false) {
                alarm = true
                activatedRef.setValue(true)
                activate_alarm_button.text =  "Stop alarm"
            }else{
                alarm = false
                activatedRef.setValue(false)
                activate_alarm_button.text =  "Start alarm"
            }

        }
        // Enables Always-on
        setAmbientEnabled()
    }

    //transform the base64 string from databas to bitmap
    fun stringToBitMap(encodedString:String) {
        try {
            val encodeByte = Base64.decode(encodedString, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.size)
            photo_from_firebase.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.message
        }
    }
}
