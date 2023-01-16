package com.example.a_track

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.a_track.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var userId: String

    //binding
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater,container,false)
        val view = binding.root;

        val dialog = LoadDialog()
        dialog.isCancelable = false

        dialog.show(parentFragmentManager, "")

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        userId = auth.currentUser!!.uid
        val userRef = firestore.collection("users")

        val userLocations = firestore.collection("locations")

        userRef.get()
            .addOnSuccessListener { result ->
                binding.registeredAssetsTv.text = result.size().toString()
            }

        userLocations.get()
            .addOnSuccessListener {
                binding.onlineAssetsTv.text = it.size().toString()
            }


        userRef.document(userId).get().addOnSuccessListener {
            val username = it.getString("username")
            val email = it.getString("email")
            val imageUrl = it.getString("imageUrl")

            Glide.with(this)
                .load(imageUrl)
                .into(binding.profileImg)

            binding.usernameTv.text = username
            binding.emailTv.text = email
            dialog.dismiss()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}