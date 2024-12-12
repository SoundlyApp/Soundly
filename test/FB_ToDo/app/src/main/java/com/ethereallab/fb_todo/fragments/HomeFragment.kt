package com.ethereallab.fb_todo.fragments


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ethereallab.fb_todo.LoginActivity
import com.ethereallab.fb_todo.adapters.TodoAdapter
import com.ethereallab.fb_todo.databinding.FragmentHomeBinding
import com.ethereallab.fb_todo.models.Todo
import com.ethereallab.fb_todo.repository.ToDoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private lateinit var repository: ToDoRepository
    private val pendingTodos = mutableListOf<Todo>()
    private lateinit var todoAdapter: TodoAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        repository = ToDoRepository(requireContext())

        todoAdapter = TodoAdapter(
            todos = pendingTodos,
            onTodoClick = { _ ->
                // Handle item clicks, e.g., marking as done
            },
            onSongClicked = { song ->

                lifecycleScope.launch {
                    // Perform the network request on the IO dispatcher
                    val tracks = fetchTracksForSong(song)
                    withContext(Dispatchers.Main) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tracks))
                        startActivity(intent)
                    }

                }
            }
        )

        // Setup RecyclerView
        binding.recyclerView.apply {
            adapter = todoAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        observeAllTodos()

        return binding.root
    }

    private suspend fun fetchTracksForSong(song: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://shazam.p.rapidapi.com/search?term=${song}&locale=en-US&offset=0&limit=5")
                    .get()
                    .addHeader("x-rapidapi-key", "4bb2c96461mshe3e6a02727dd211p11544ajsna0e8daf29720")
                    .addHeader("x-rapidapi-host", "shazam.p.rapidapi.com")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("API Error", "Unexpected code $response")
                        return@withContext null
                    }

                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val json = JSONObject(responseBody)
                        val tracks = json.optJSONObject("tracks")
                        val hitsArray = tracks.optJSONArray("hits")
                        val firstHit = hitsArray.optJSONObject(0)
                        val track = firstHit.optJSONObject("track")
                        val share = track.optJSONObject("share")
                        val href = share.optString("href", null)
                        return@withContext href
                    } else {
                        Log.e("Response Error", "Response body is null")
                        return@withContext null
                    }
                }
            } catch (e: Exception) {
                Log.e("Network Error", e.message ?: "Unknown error")
                return@withContext null
            }
        }
    }

    private fun observeAllTodos(){
        lifecycleScope.launch {
            repository.getAllTodos().collect { todos ->
                if (_binding != null) { // Check if binding is still valid
                    pendingTodos.clear()
                    pendingTodos.addAll(todos.reversed())
                    todoAdapter.notifyDataSetChanged()

                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
