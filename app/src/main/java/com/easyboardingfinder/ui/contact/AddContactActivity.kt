package com.easyboardingfinder.ui.contact

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.easyboardingfinder.data.model.ContactMessage
import com.easyboardingfinder.data.repository.ContactRepository
import com.easyboardingfinder.databinding.ActivityAddContactBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AddContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddContactBinding
    private val repository = ContactRepository()
    private val auth = FirebaseAuth.getInstance()
    private var editId: String? = null
    private var existingMessage: ContactMessage? = null

    private var recipientId: String = ""
    private var itemTitle: String = ""
    private var itemType: String = ""
    private var itemId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editId = intent.getStringExtra("EDIT_ID")
        recipientId = intent.getStringExtra("RECIPIENT_ID") ?: ""
        itemTitle = intent.getStringExtra("ITEM_TITLE") ?: ""
        itemType = intent.getStringExtra("ITEM_TYPE") ?: ""
        itemId = intent.getStringExtra("ITEM_ID") ?: ""

        setupClickListeners()
        prefillUserData()

        if (itemTitle.isNotEmpty()) {
            binding.tvTitle.text = "Send Inquiry"
            binding.etSubject.setText("Inquiry: $itemTitle")
            binding.btnSubmit.text = "Send Inquiry"
        }

        if (editId != null) {
            binding.tvTitle.text = "Edit Message"
            binding.btnSubmit.text = "Update Message"
            loadExistingData()
        }
    }

    private fun prefillUserData() {
        val user = auth.currentUser
        user?.let {
            binding.etName.setText(it.displayName ?: "")
            binding.etEmail.setText(it.email ?: "")
        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener { finish() }

        binding.btnSubmit.setOnClickListener {
            if (validateInputs()) {
                saveMessage()
            }
        }
    }

    private fun loadExistingData() {
        lifecycleScope.launch {
            try {
                val message = repository.getMessageById(editId!!)
                message?.let {
                    existingMessage = it
                    binding.etName.setText(it.name)
                    binding.etEmail.setText(it.email)
                    binding.etPhone.setText(it.phone)
                    binding.etSubject.setText(it.subject)
                    binding.etMessage.setText(it.message)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddContactActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (binding.etName.text.toString().trim().isEmpty()) {
            binding.etName.error = "Name is required"
            return false
        }
        if (binding.etEmail.text.toString().trim().isEmpty()) {
            binding.etEmail.error = "Email is required"
            return false
        }
        if (binding.etSubject.text.toString().trim().isEmpty()) {
            binding.etSubject.error = "Subject is required"
            return false
        }
        if (binding.etMessage.text.toString().trim().isEmpty()) {
            binding.etMessage.error = "Message is required"
            return false
        }

        val phone = binding.etPhone.text.toString().trim()
        if (phone.isNotEmpty()) {
            var cleanedPhone = phone.replace("\\s".toRegex(), "").trim()
            if (cleanedPhone.startsWith("+94")) {
                cleanedPhone = "0" + cleanedPhone.substring(3)
            } else if (cleanedPhone.startsWith("94") && cleanedPhone.length == 11) {
                cleanedPhone = "0" + cleanedPhone.substring(2)
            }
            if (cleanedPhone.length != 10 || !cleanedPhone.all { it.isDigit() }) {
                binding.etPhone.error = "Phone number must be exactly 10 digits"; return false
            }
            binding.etPhone.setText(cleanedPhone)
        }

        return true
    }

    private fun saveMessage() {
        binding.btnSubmit.isEnabled = false

        val message = existingMessage?.copy(
            name = binding.etName.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            subject = binding.etSubject.text.toString().trim(),
            message = binding.etMessage.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim()
        ) ?: ContactMessage(
            id = editId ?: "",
            name = binding.etName.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            subject = binding.etSubject.text.toString().trim(),
            message = binding.etMessage.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim(),
            userId = auth.currentUser?.uid ?: "",
            recipientId = recipientId,
            itemTitle = itemTitle,
            itemType = itemType,
            itemId = itemId
        )

        lifecycleScope.launch {
            try {
                if (editId != null) {
                    repository.updateMessage(message)
                    Toast.makeText(this@AddContactActivity, "Updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    repository.addMessage(message)
                    Toast.makeText(this@AddContactActivity, "Message sent!", Toast.LENGTH_SHORT).show()
                }
                finish()
            } catch (e: Exception) {
                binding.btnSubmit.isEnabled = true
                Toast.makeText(this@AddContactActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
