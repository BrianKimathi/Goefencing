package com.example.a_track

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment

class LoadDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity)
        val inflater = requireActivity().layoutInflater
        val dialog = inflater.inflate(R.layout.load_dialog, null)

        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        return builder.create()
    }

}