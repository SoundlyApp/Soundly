package com.ethereallab.fb_todo.fragments

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ethereallab.fb_todo.R
import com.ethereallab.fb_todo.adapters.TodoAdapter
import com.ethereallab.fb_todo.databinding.FragmentPendingBinding
import com.ethereallab.fb_todo.models.Todo
import com.ethereallab.fb_todo.repository.ToDoRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import com.google.firebase.firestore.Blob

class PendingFragment : Fragment() {

    private var _binding: FragmentPendingBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: ToDoRepository
    private val pendingTodos = mutableListOf<Todo>()
    private lateinit var todoAdapter: TodoAdapter
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var selectedImageData : ByteArray? = null;

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPendingBinding.inflate(inflater, container, false)
        repository = ToDoRepository(requireContext())

        // Setup RecyclerView with click listener
        todoAdapter = TodoAdapter(
            todos = pendingTodos,
            onTodoClick = { todo ->
                showConfirmationDialog(todo)
            },
            onSongClicked = { songName ->
                // Handle song clicks or leave empty if not needed
                Toast.makeText(requireContext(), "Song clicked: $songName", Toast.LENGTH_SHORT).show()
            }
        )

        binding.addImage.setOnClickListener {
            showImageSelectionDialog()
        }

        // Add new TODO item
        binding.addTodoButton.setOnClickListener {
            val newContent = binding.todoInputEditText.text.toString().trim()
            val newSong = binding.musicInput.text.toString()


            if (newContent.isNotEmpty()) {
                val currentUserId = auth.currentUser?.uid
                val currentUsername = auth.currentUser?.displayName


                if (currentUserId != null) {

                    var newTodo = Todo(content = newContent, userId = currentUserId, username = currentUsername, song = newSong, imageUrl = selectedImageData )
                    lifecycleScope.launch {
                        repository.addTodo(newTodo)
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, HomeFragment())
                            .commit()

                    }

                    val imageBlob = selectedImageData?.let { Blob.fromBytes(it) }
                    val todoData = hashMapOf(
                        "content" to newContent,
                        "userId" to currentUserId,
                        "username" to currentUsername,
                        "song" to newSong,
                        "imageBlob" to imageBlob // Storing as a Blob, not as a ByteArray
                    )
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    firestore.collection("todos")
                        .add(todoData)


                    binding.todoInputEditText.text?.clear()
                    binding.musicInput.text?.clear()
                    binding.currentImage.setImageDrawable(null)
                    binding.currentImage.visibility = View.GONE
                    selectedImageData = null

                } else {
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(context, "Please enter a TODO item", Toast.LENGTH_SHORT).show()
            }
        }


        return binding.root
    }

    private fun showImageSelectionDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private val cameraLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // You have the bitmap from the camera
            Log.d("CAMERA", bitmap.toString());
            selectedImageData = bitmapToByteArray(bitmap)
            binding.currentImage.setImageBitmap(bitmap)
            binding.currentImage.visibility = View.VISIBLE
        }
    }

    private val galleryLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            Log.d("CAMERA", uri.toString());
            // You have the URI from the gallery
//            handleImageUri(uri)
            Glide.with(requireContext())
                .load(uri)
                .into(binding.currentImage)

            val bitmap = uriToBitmap(uri);
            if (bitmap != null){
                selectedImageData = bitmapToByteArray(bitmap)
            }

            binding.currentImage.visibility = View.VISIBLE;
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun openCamera() {
        // This will open the camera and return a bitmap through cameraLauncher
        cameraLauncher.launch(null)
    }

    private fun openGallery() {
        // This will open the gallery and return a uri through galleryLauncher
        galleryLauncher.launch("image/*")
    }

    private fun showConfirmationDialog(todo: Todo) {
        AlertDialog.Builder(requireContext())
            .setTitle("Mark as Completed")
            .setMessage("Are you sure you want to mark this TODO as completed?")
            .setPositiveButton("Yes") { _, _ -> updateTodoStatus(todo, true) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updateTodoStatus(todo: Todo, isDone: Boolean) {
        lifecycleScope.launch {
            val updatedTodo = todo.copy(isDone = isDone, completedDate = if (isDone) System.currentTimeMillis() else null)
            repository.updateTodoStatus(updatedTodo)
            Toast.makeText(context, "Todo updated successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
