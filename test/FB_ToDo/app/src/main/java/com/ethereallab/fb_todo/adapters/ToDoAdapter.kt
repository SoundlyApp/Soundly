// TodoAdapter.kt
package com.ethereallab.fb_todo.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View

import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.ethereallab.fb_todo.databinding.ItemTodoBinding
import com.ethereallab.fb_todo.models.Todo

class TodoAdapter(
    private val todos: List<Todo>,
    private val onTodoClick: (Todo) -> Unit, // Click listener as a lambda
    private val onSongClicked: (String) -> Unit,
    private val t: String = "BQDQgaDXt9jTLD761mp0aKu9qkebv19dRB3Oo7mzcddcZnEyKVeExOo0Ct_mhwaJAHsCHRtg4hLfxL8anaIUV0dODtaRj-4AGJQCaZHmjknPgWC86jTaqaqWEIkm7bGMNW0uahi_IpKJk5a1FYOykibQGylRa0NkVfMFhWQ3LZ4VLfG-C-QXhjd5091DQ8WhC5o34p6qc6lFFCMQ4A0"
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    inner class TodoViewHolder(val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(todo: Todo) {
            binding.nameTextView.text = todo.username
            binding.contentTextView.text = todo.content
            binding.userProfile.text = todo.username?.first()?.uppercaseChar().toString()

            if (todo.imageUrl != null) {
                val byteArray = todo.imageUrl
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
                binding.imageView.visibility = View.VISIBLE
            } else {
                // No image
                binding.imageView.visibility = View.GONE
            }

            if (todo.song?.isNotEmpty() == true){
                binding.musicTextView.text = "Listening to: " + todo.song;
                binding.musicTextView.setOnClickListener {
                    onSongClicked(todo.song)
                }
            } else {
                binding.musicTextView.visibility = View.GONE;

            }

            binding.root.setOnClickListener { onTodoClick(todo) } // Set click listener
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(todos[position])
    }

    override fun getItemCount(): Int = todos.size
}
