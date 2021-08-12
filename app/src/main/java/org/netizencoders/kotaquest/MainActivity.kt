package org.netizencoders.kotaquest

import android.content.Intent //launch activity
import android.os.Bundle //used to pass data between activities
import android.util.Log //API for sending log output
import android.widget.TextView //A user interface element that displays text to the user
import androidx.appcompat.app.AppCompatActivity
//Base class for activities that wish to use some of the newer platform features on older Android devices
import com.google.android.material.bottomnavigation.BottomNavigationView //Represents a standard bottom navigation bar for application
import com.google.firebase.firestore.ktx.firestore //connect to Firestore
import com.google.firebase.ktx.Firebase //connect to Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var tv: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    private val db = Firebase.firestore

    private val user = hashMapOf(
        "first" to "Elmerulia",
        "last" to "Frixell",
        "born" to 2020
    )

    private var status = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.h1 -> move("1")
                R.id.h2 -> move("2")
                R.id.h3 -> move("3")
            }
            true
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun move(nav: String) {
        when (nav) {
            "1" -> {
                val intent = Intent(this,ActivityListQuests::class.java)
                startActivity(intent)
                finish()
            }
            "2" -> {
                val intent = Intent(this,ActivityListQuestsTaken::class.java)
                startActivity(intent)
                finish()
            }
            "3" -> {
                val intent = Intent(this,ActivityNewQuest::class.java)
                startActivity(intent)
                finish()
            }
        }
    }


    private fun addData() {
        db.collection("users")
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d("", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("", "Error adding document", e)
            }
    }

    private fun readData() {
        status = ""
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d("", "${document.id} => ${document.data}")
                    status += "${document.id} => ${document.data} \n"
                    tv.text = status
                }
            }
            .addOnFailureListener { exception ->
                Log.w("", "Error getting documents.", exception)
            }
        Log.d("", status)
    }
}