package com.easyboardingfinder.ui.contact

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.easyboardingfinder.data.model.ContactMessage
import com.easyboardingfinder.data.repository.ContactRepository
import com.easyboardingfinder.databinding.FragmentContactBinding
import com.easyboardingfinder.ui.adapter.ContactAdapter
import com.easyboardingfinder.ui.main.MainActivity
import com.easyboardingfinder.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ContactFragment : Fragment() {

    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    private val repository = ContactRepository()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: ContactAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        loadData()

        binding.btnBack.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_home)
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupRecyclerView() {
        val userId = auth.currentUser?.uid ?: ""
        adapter = ContactAdapter(
            currentUserId = userId,
            onEditClick = { message ->
                val intent = Intent(requireContext(), AddContactActivity::class.java)
                intent.putExtra("EDIT_ID", message.id)
                startActivity(intent)
            },
            onDeleteClick = { message -> confirmDelete(message) },
            onReplyClick = { message -> showReplyDialog(message) },
            onItemClick = { /* Detail view */ }
        )
        val sharedPrefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        val role = sharedPrefs.getString("user_role", "USER") ?: "USER"
        adapter.currentUserRole = role

        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMessages.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddContactActivity::class.java))
        }
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        val userId = auth.currentUser?.uid ?: ""

        lifecycleScope.launch {
            try {
                val allMessages = repository.getAllMessages()
                val sharedPrefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
                val role = sharedPrefs.getString("user_role", "USER") ?: "USER"

                val filteredMessages = if (role == "ADMIN") {
                    allMessages
                } else {
                    allMessages.filter { msg ->
                        msg.userId == userId || msg.recipientId == userId
                    }
                }

                adapter.updateData(filteredMessages)
                binding.progressBar.visibility = View.GONE
                binding.emptyState.visibility = if (filteredMessages.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReplyDialog(message: ContactMessage) {
        val context = requireContext()
        val input = android.widget.EditText(context).apply {
            hint = "Type your reply here..."
            setText(message.reply)
            setPadding(48, 48, 48, 48)
        }

        AlertDialog.Builder(context)
            .setTitle("Reply to Message")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val replyText = input.text.toString().trim()
                if (replyText.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            repository.updateMessage(message.copy(reply = replyText))
                            Toast.makeText(context, "Reply sent!", Toast.LENGTH_SHORT).show()
                            loadData()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to send reply: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(message: ContactMessage) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteMessage(message.id)
                        Toast.makeText(requireContext(), "Deleted successfully", Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
