package com.example.a_track

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.startActivity
import com.example.a_track.databinding.ActivityRegisterBinding
import com.example.a_track.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var imageUri: Uri
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var imageUrl: Uri

    val imgRef = Firebase.storage.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val getImg = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            ActivityResultCallback {
                it?.let {
                    imageUri = it
                }
                binding.profileImg.setImageURI(imageUri)
            }
        )

        binding.pickImgBtn.setOnClickListener { getImg.launch("image/*") }

        binding.registerBtn.setOnClickListener { registerUser() }
        binding.goToRLoginTv.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

    }

    private fun registerUser() {
        val email = binding.emailRegisterEt.text.toString()
        val password = binding.passwordRegisterEt.text.toString()
        val username = binding.usernameRegisterEt.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main){
                    binding.progressBar.visibility = View.VISIBLE
                }
                try {
                    auth.createUserWithEmailAndPassword(email, password).await()
                    imageUri.let {
                        imgRef.child("images/${auth.currentUser!!.uid}").putFile(imageUri).await()
                        imageUrl = imgRef.child("images/${auth.currentUser!!.uid}").downloadUrl.await()
                    }
                    db.collection("users").document(auth.currentUser!!.uid)
                        .set(User(username, email, imageUrl))
                        .await()
                    withContext(Dispatchers.Main){
                        binding.progressBar.visibility = View.GONE
                        checkLoggedInState()
                    }
                }catch (e: Exception){
                    withContext(Dispatchers.Main){
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@RegisterActivity, "Error ${e.message} occurred!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

        }
    }



    private fun checkLoggedInState() {
        if (auth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}