package org.netizencoders.kotaquest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import org.netizencoders.kotaquest.helpers.ListDataAdapter
import org.netizencoders.kotaquest.models.Quest

class ActivityListQuests : AppCompatActivity() {
    private lateinit var progressbar: ProgressBar
    private lateinit var noDataLabel: TextView
    private lateinit var quests: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private var data: ArrayList<Quest> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listquests)

        supportActionBar?.hide()

        quests = findViewById(R.id.quest_list_items)
        quests.setHasFixedSize(true)
        progressbar = findViewById(R.id.quest_list_progressbar)
        noDataLabel = findViewById(R.id.quest_list_no_data)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.h1
        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.h1 -> move("1")
                R.id.h2 -> move("2")
                R.id.h3 -> move("3")
            }
            true
        }

        getData()
    }

    override fun onRestart() {
        super.onRestart()
        recreate()
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
                val intent = Intent(this,ActivityPostedQuests::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun getData() {
        data.clear()

        noDataLabel.visibility = View.INVISIBLE
        quests.visibility = View.INVISIBLE
        progressbar.visibility = View.VISIBLE

        val db = FirebaseFirestore.getInstance()
        db.collection("quests")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (document.data["Status"].toString()=="Posted") {
                        Log.d("", document.toString())
                        val quest = Quest(
                            document.reference.id,
                            document.data["Title"].toString(),
                            document.data["Location"].toString(),
                            document.data["Description"].toString(),
                            document.data["ImageURL"].toString(),
                            document.data["Status"].toString(),
                            document.data["Poster"].toString(),
                            document.data["Quester"].toString(),
                            document.data["DatePosted"].toString(),
                            document.data["DateCompleted"].toString(),
                            document.data["ReportDescription"].toString(),
                            document.data["ReportImageURL"].toString()
                        )
                        data.add(quest)
                    }
                }
                if (data.isNotEmpty()) {
                    noDataLabel.visibility = View.INVISIBLE
                    showRecyclerList(data)
                    quests.visibility = View.VISIBLE
                    progressbar.visibility = View.INVISIBLE
                } else {
                    noDataLabel.visibility = View.VISIBLE
                    progressbar.visibility = View.INVISIBLE
                }
            }
            .addOnFailureListener { exception ->
                Log.d("", "Error: \n $exception")
                recreate()
            }
    }

    private fun showRecyclerList(data: ArrayList<Quest>) {
        quests.setHasFixedSize(true)
        quests.layoutManager = LinearLayoutManager(this)
        val listItemAdapter = ListDataAdapter(data)
        quests.adapter = listItemAdapter

        listItemAdapter.notifyDataSetChanged()
        
        listItemAdapter.setOnItemBtnClickCallback(object : ListDataAdapter.OnItemBtnClickCallback {
            override fun onItemBtnClicked(data: Quest, button: String) {
                when (button) {
                    "btnR" -> {
                        takeQuest(data)
                    }
                }
            }
        })
    }

    private fun takeQuest(quest: Quest) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(quest.Title)
        builder.setMessage("Are you sure you want to take this Quest?")

        builder.setPositiveButton(
            "Yes") { _, _ ->
            updateQuest("quests/"+quest.ID.toString(), "Status", "In Progress")
            updateQuest("quests/"+quest.ID.toString(), "Quester", ActivityLogin.uid)

            val moveIntent = Intent(this, ActivityListQuestsTaken::class.java)
            startActivity(moveIntent)
        }

        builder.setNegativeButton(
            "No") { _, _ ->
        }

        builder.show()
    }

    private fun updateQuest(path: String, Field: String, Value: String) {
        val db = FirebaseFirestore.getInstance()
        db.document(path)
            .update(Field, Value)
            .addOnSuccessListener {
                Log.d("", "Update Success")
            }
            .addOnFailureListener { e ->
                Log.d("", "Error $e")
                Toast.makeText(this, "Error: $e", Toast.LENGTH_LONG).show()
            }
        }
}
