package org.netizencoders.kotaquest

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import org.netizencoders.kotaquest.models.Quest
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


var storage: FirebaseStorage? = null
var storageReference: StorageReference? = null


class ActivityNewQuest : AppCompatActivity() {
    private lateinit var progressbar: ProgressBar
    private lateinit var view: ConstraintLayout
    private lateinit var imageView: ImageView
    private lateinit var btnChoose: Button
    private lateinit var btnCancel: Button
    private lateinit var btnContinue: Button
    private lateinit var qTitle: EditText
    private lateinit var qLocation: EditText
    private lateinit var qDescription: EditText
    private lateinit var qImageURL: String

    private var filePath: Uri? = null
    private val PICK_IMAGE_REQUEST = 71

    private lateinit var imagePicker: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_newquest)

        supportActionBar?.hide()

        storage = FirebaseStorage.getInstance()
        storageReference = storage!!.reference
        imagePicker =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                onActivityResult(PICK_IMAGE_REQUEST, result)
            }

        view = findViewById(R.id.new_quest_full)
        progressbar = findViewById(R.id.new_quest_progressbar)
        imageView = findViewById(R.id.new_quest_image)
        btnChoose = findViewById(R.id.new_quest_choose)
        btnCancel = findViewById(R.id.new_quest_cancel)
        btnContinue = findViewById(R.id.new_quest_post)
        qTitle = findViewById(R.id.new_quest_title)
        qLocation = findViewById(R.id.new_quest_location)
        qDescription = findViewById(R.id.new_quest_description)

        btnChoose.setOnClickListener {
            chooseImage()
        }

        btnCancel.setOnClickListener {
            val moveIntent = Intent(this, ActivityListQuests::class.java)
            startActivity(moveIntent)
        }

        btnContinue.setOnClickListener {
            commit()
        }
    }

    private fun commit() {
        Toast.makeText(this, "Processing...", Toast.LENGTH_LONG).show()

        view.visibility = View.INVISIBLE
        progressbar.visibility = View.VISIBLE

        if (filePath != null) {
            uploadImage()
        } else {
            qImageURL = ""
            prepareQuest()
        }
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
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    if (intent != null) {
                        filePath = intent.data
                    }
                    val bitmap: Bitmap
                    val contentResolver = contentResolver
                    try {
                        bitmap = if (Build.VERSION.SDK_INT < 28) {
                            MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                        } else {
                            val source = ImageDecoder.createSource(contentResolver, filePath!!)
                            ImageDecoder.decodeBitmap(source)
                        }
                        imageView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
                    prepareQuest()
                }
            }?.addOnFailureListener {
                Toast.makeText(this, "Error: " + it.localizedMessage, Toast.LENGTH_LONG).show()

                view.visibility = View.VISIBLE
                progressbar.visibility = View.INVISIBLE
            }
        }
    }

    private fun sha256(str: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(str.toByteArray(UTF_8))

    private fun prepareQuest() {
        val simpleDateFormat = SimpleDateFormat.getDateTimeInstance()
        val currentDateAndTime: String = simpleDateFormat.format(Date())

        val quest = Quest(
            sha256(ActivityLogin.uid+" "+currentDateAndTime).toString(), qTitle.text.toString(), qLocation.text.toString(), qDescription.text.toString(),
            qImageURL, "Posted", ActivityLogin.uid, "", currentDateAndTime, "", "", ""
        )

        val data: HashMap<String, Any> = HashMap()
        data["ID"] = quest.ID.toString()
        data["Title"] = quest.Title.toString()
        data["Location"] = quest.Location.toString()
        data["Description"] = quest.Description.toString()
        data["ImageURL"] = quest.ImageURL.toString()
        data["Status"] = quest.Status.toString()
        data["Poster"] = quest.Poster.toString()
        data["Quester"] = quest.Quester.toString()
        data["DatePosted"] = quest.DatePosted.toString()
        data["DateCompleted"] = quest.DateCompleted.toString()
        data["ReportDescription"] = quest.ReportDescription.toString()
        data["ReportImageURL"] = quest.ReportImageURL.toString()

        postQuest(data)
    }


    private fun postQuest(data: HashMap<String, Any>) {
        if (!data.isNullOrEmpty()) {
            val db = FirebaseFirestore.getInstance()
            db.collection("quests")
                .document(data["ID"].toString())
                .set(data)
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        "Quest posted",
                        Toast.LENGTH_LONG
                    ).show()

                    val moveIntent = Intent(this, ActivityListQuests::class.java)
                    startActivity(moveIntent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: $e", Toast.LENGTH_LONG).show()

                    view.visibility = View.VISIBLE
                    progressbar.visibility = View.INVISIBLE
                }
        }
    }
}