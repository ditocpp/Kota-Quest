package org.netizencoders.kotaquest

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import org.netizencoders.kotaquest.models.Quest
import java.util.*

class ActivityReportQuest : AppCompatActivity() {
    private lateinit var progressbar: ProgressBar
    private lateinit var view: ConstraintLayout
    private lateinit var qImageView: ImageView
    private lateinit var btnChoose: Button
    private lateinit var btnCancel: Button
    private lateinit var btnContinue: Button
    private lateinit var qTitle: TextView
    private lateinit var qDescription: EditText
    private lateinit var qImageURL: String
    private lateinit var quest: Quest

    private var filePath: Uri? = null
    private val PICK_IMAGE_REQUEST = 71

    private lateinit var imagePicker: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportquest)

        supportActionBar?.hide()

        storage = FirebaseStorage.getInstance()
        storageReference = storage!!.reference
        imagePicker =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                onActivityResult(PICK_IMAGE_REQUEST, result)
            }

        view = findViewById(R.id.report_quest_full)
        progressbar = findViewById(R.id.report_quest_progressbar)
        qImageView = findViewById(R.id.report_quest_image)
        btnChoose = findViewById(R.id.report_quest_choose)
        btnCancel = findViewById(R.id.report_quest_cancel)
        btnContinue = findViewById(R.id.report_quest_finish)
        qTitle = findViewById(R.id.report_quest_title)
        qDescription = findViewById(R.id.report_quest_description)

        btnChoose.setOnClickListener {
            chooseImage()
        }

        btnCancel.setOnClickListener {
            val moveIntent = Intent(this, ActivityListQuestsTaken::class.java)
            startActivity(moveIntent)
        }

        btnContinue.setOnClickListener {
            commit()
        }

        getData()

        progressbar.visibility = View.INVISIBLE
    }

    private fun commit() {
        Toast.makeText(this, "Processing...", Toast.LENGTH_LONG).show()

        view.visibility = View.INVISIBLE
        progressbar.visibility = View.VISIBLE

        if (filePath != null) {
            uploadImage()
        } else {
            report()
        }
    }

    private fun report() {
        updateQuest("quests/"+quest.ID.toString(), "Status", "Reported")
        updateQuest("quests/"+quest.ID.toString(), "ReportDescription",
            qDescription.text.toString()
        )
        Toast.makeText(
            this,
            "Quest reported",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(this,ActivityListQuestsTaken::class.java)
        startActivity(intent)
        finish()
    }

    private fun getData() {
        progressbar.visibility = View.VISIBLE

        val db = FirebaseFirestore.getInstance()
        db.collection("quests").document(ActivityListQuestsTaken.qid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d("", document.toString())
                    quest = Quest(
                        document.reference.id,
                        document.data?.get("Title").toString(),
                        document.data?.get("Location").toString(),
                        document.data?.get("Description").toString(),
                        document.data?.get("ImageURL").toString(),
                        document.data?.get("Status").toString(),
                        document.data?.get("Poster").toString(),
                        document.data?.get("Quester").toString(),
                        document.data?.get("DatePosted").toString(),
                        document.data?.get("DateCompleted").toString(),
                        document.data?.get("ReportDescription").toString(),
                        document.data?.get("ReportImageURL").toString()
                    )

                    qTitle.text = quest.Title
                }
            }
            .addOnFailureListener { exception ->
                Log.d("", "Error: \n $exception")
                recreate()
            }

        progressbar.visibility = View.INVISIBLE
    }

    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        imagePicker.launch(
            Intent.createChooser(
                intent,
                "Select a picture"
            )
        )
    }

    private fun onActivityResult(requestCode: Int, result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            Log.d("aaaa", "a")
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    Log.d("aaaa", "b")
                    if (intent != null) {
                        filePath = intent.data
                        Log.d("aaaa", filePath.toString())
                    }
                    val bitmap: Bitmap
                    val contentResolver = contentResolver
                    try {
                        Log.d("aaaa", "d")
                        bitmap = if (Build.VERSION.SDK_INT < 28) {
                            MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                        } else {
                            val source = ImageDecoder.createSource(contentResolver, filePath!!)
                            ImageDecoder.decodeBitmap(source)
                        }
                        qImageView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        Log.d("aaaa", "f")
                        e.printStackTrace()
                    }
                    Log.d("aaaa", "e")
                }
            }
        }
    }

    private fun uploadImage() {
        if (filePath != null) {
            val ref = storageReference?.child("images/" + UUID.randomUUID().toString())
            val uploadTask = ref?.putFile(filePath!!)

            uploadTask?.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                return@Continuation ref.downloadUrl
            })?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    qImageURL = downloadUri.toString()
                    updateQuest("quests/"+quest.ID.toString(), "ReportImageURL", qImageURL)
                    report()
                }
            }?.addOnFailureListener {
                Toast.makeText(this, "Error: " + it.localizedMessage, Toast.LENGTH_LONG).show()

                view.visibility = View.VISIBLE
                progressbar.visibility = View.INVISIBLE
            }
        }
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