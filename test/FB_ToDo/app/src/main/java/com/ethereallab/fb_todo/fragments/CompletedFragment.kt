package com.ethereallab.fb_todo.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ethereallab.fb_todo.adapters.TodoAdapter
import com.ethereallab.fb_todo.databinding.FragmentCompletedBinding
import com.ethereallab.fb_todo.models.Todo
import com.ethereallab.fb_todo.repository.ToDoRepository
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.RequestParams
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import com.ethereallab.fb_todo.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request

class CompletedFragment : Fragment() {

    private var _binding: FragmentCompletedBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: ToDoRepository
    private val completedTodos = mutableListOf<Todo>()
    private lateinit var todoAdapter: TodoAdapter
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val t = "BQDQgaDXt9jTLD761mp0aKu9qkebv19dRB3Oo7mzcddcZnEyKVeExOo0Ct_mhwaJAHsCHRtg4hLfxL8anaIUV0dODtaRj-4AGJQCaZHmjknPgWC86jTaqaqWEIkm7bGMNW0uahi_IpKJk5a1FYOykibQGylRa0NkVfMFhWQ3LZ4VLfG-C-QXhjd5091DQ8WhC5o34p6qc6lFFCMQ4A0";

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompletedBinding.inflate(inflater, container, false)
        repository = ToDoRepository(requireContext())

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        val user = auth.currentUser
        val email = user?.email
        val username = user?.displayName
        val firstLetter = username?.first()?.uppercaseChar()


        binding.userProfile.text = firstLetter.toString()
        binding.usernameView.text = username
        binding.emailView.text = email

        // Setup RecyclerView with an item click listener to un-complete the todo
        todoAdapter = TodoAdapter(
            todos = completedTodos,
            onTodoClick = { todo -> showMarkNotDoneDiagog(todo) },
            onSongClicked = { _ ->


            }
        )
        binding.recyclerView.apply {
            adapter = todoAdapter
            layoutManager = LinearLayoutManager(context)
        }

        observeCompletedTodos()

        return binding.root
    }

    private fun observeCompletedTodos() {
        lifecycleScope.launch {
            repository.getUserTodos().collect { todos ->
                if (_binding != null) { // Check if binding is still valid
                    completedTodos.clear()
                    completedTodos.addAll(todos.reversed())
                    todoAdapter.notifyDataSetChanged()

                    // Show emptyTextView if the list is empty
                    binding.emptyTextView.visibility = if (todos.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    // Show confirmation dialog for un-completing a todo
    private fun showMarkNotDoneDiagog(todo: Todo) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete post")
            .setMessage("Do you want to delete this post?")
            .setPositiveButton("Yes") { _, _ ->
                markToDoNotDone(todo)
            }
            .setNegativeButton("No", null)
            .show()
    }
    private fun markToDoNotDone(todo: Todo) {
        val updatedTodo = todo.copy(isDone = false, completedDate = null) // Mark as pending
        lifecycleScope.launch {
            try {
                repository.updateTodoStatus(updatedTodo)
                Toast.makeText(requireContext(), "TODO marked as pending", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to mark TODO as pending", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
