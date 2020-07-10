package com.nemesis.todolist.taskcreation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nemesis.todolist.R
import com.nemesis.todolist.login.LoginActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_create_todo.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class CreateTodoActivity : AppCompatActivity() {

    companion object {
        fun getLaunchIntent(from: Context) = Intent(from, CreateTodoActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        private lateinit var context: Context

        fun setContext(con: Context) {
            context = con
        }

        fun getContext(): Context {
            return context
        }

        const val TAG = "CreateTodoActivity"
        private val uid = FirebaseAuth.getInstance().uid ?: ""
        val adapter = GroupAdapter<GroupieViewHolder>()

        /**
         * Current date is returned
         *
         * @return String containing formatted Date
         */
        fun getDate(): String {
            val currentTime = LocalDateTime.now()
            val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            return currentTime.format(dateFormat)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_todo)
        setupUI()
        fetchTodos()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.sign_out_symbol -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Signs out the user from their Google Account
     */
    private fun signOut() {
        startActivity(LoginActivity.getLaunchIntent(this))
        FirebaseAuth.getInstance().signOut()
    }

    /**
     * Setups UI of activity
     */
    private fun setupUI() {
        setContext(this)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        recyclerview_todos.adapter = adapter
        recyclerview_todos.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        add_todo_fab.setOnClickListener {
            val bottomSheetView = View.inflate(this, R.layout.bottom_sheet, null)
            val bottomSheetDialog = BottomSheetDialog(this)
            bottomSheetDialog.setContentView(bottomSheetView)

            val imm =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            bottomSheetDialog.setCanceledOnTouchOutside(false)
            bottomSheetDialog.et_add_todo.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    imm.hideSoftInputFromWindow(bottomSheetDialog.et_add_todo.windowToken, 0)
                    //bottomSheetDialog.dismiss()
                    true
                } else {
                    false
                }
            }

            // Empty text bug

            priorityLevels(bottomSheetDialog)
            bottomSheetDialog.add_todo_btn.setOnClickListener {
                if (bottomSheetDialog.et_add_todo.text.toString() == "") {
                    Log.d(TAG, "Please enter your todo")
                } else if (!bottomSheetDialog.low_btn.isSelected && !bottomSheetDialog.medium_btn.isSelected
                    && !bottomSheetDialog.high_btn.isSelected
                ) {
                    Log.d(TAG, "Please select priority")
                } else {
                    Log.d(TAG, "Adding todo")
                    var selectedPriority = ""
                    selectedPriority = when {
                        bottomSheetDialog.low_btn.isSelected -> {
                            bottomSheetDialog.low_btn.text.toString()
                        }
                        bottomSheetDialog.medium_btn.isSelected -> {
                            bottomSheetDialog.medium_btn.text.toString()
                        }
                        else -> {
                            bottomSheetDialog.high_btn.text.toString()
                        }
                    }
                    Log.d(TAG, selectedPriority)
                    addToList(bottomSheetDialog.et_add_todo.text.toString(), selectedPriority)
                    bottomSheetDialog.dismiss()
                }
            }
            bottomSheetDialog.show()
        }
    }

    /**
     * Checks if todos have already been fetched from Firestore
     *
     * @return Boolean value of if data has already been fetched or not
     */
    private fun checkTodosAlreadyFetched(): Boolean {
        return adapter.itemCount >= 1
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_action_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Adds the todo to the list of todos
     * @param todo String the todo the user enters
     */
    private fun addToList(todo: String, priority: String) {
        var duplicateFound = false
        if (adapter.groupCount == 0) {
            addTodo(todo, priority)
            return
        } else {
            for (x in 0 until adapter.groupCount) {
                // Check if list about to be added already existed
                val itemToCheck = adapter.getItem(x) as NewTodoActivity
                if (itemToCheck.textTest == todo) {
                    duplicateFound = true
                }
            }
        }
        if (!duplicateFound) {
            addTodo(todo, priority)
        } else {
            Toast.makeText(this, "That List already exists", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Fetches users current list of todos
     */
    private fun fetchTodos() {
        if (!checkTodosAlreadyFetched()) {
            val db = Firebase.firestore
            // Fetches all documents from the todos collection within Firestore
            db.collection("todolist").document(uid).collection("todos")
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        Log.d(TAG, document.getString("priority").toString())
                        adapter.add(
                            NewTodoActivity(
                                document.get("todo").toString(),
                                document.get("isCompleted") as Boolean,
                                document.getString("priority").toString(),
                                document.get("creationDate").toString(),
                                document.get("updatedDate").toString(),
                                document.id
                            )
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
        }
    }

    /**
     *
     * @param todo is the Todo value that the user enters
     */
    private fun addTodo(todo: String, priority: String) {
        val db = Firebase.firestore
        val date = getDate()
        if (uid != "") {
            // Fields of document to be stored in Firestore document
            val todoList = hashMapOf(
                "todo" to todo,
                "isCompleted" to false,
                "priority" to priority,
                "creationDate" to date,
                "updatedDate" to ""
            )
            // Firestore path of todolist placement
            db.collection("todolist").document(uid).collection("todos")
                .add(todoList)
                .addOnSuccessListener { documentReference ->
                    adapter.add(
                        NewTodoActivity(
                            todo,
                            false,
                            priority,
                            date,
                            "",
                            documentReference.id
                        )
                    )
                }
                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
        }
    }


    /**
     * Set on click listeners for todo priority levels
     * @param bottomSheetDialog BottomSheetDialog for adding todos
     */
    private fun priorityLevels(bottomSheetDialog: BottomSheetDialog) {
        bottomSheetDialog.low_btn.setOnClickListener {
            resetSelectionPriorityLevel(bottomSheetDialog.medium_btn)
            resetSelectionPriorityLevel(bottomSheetDialog.high_btn)
            bottomSheetDialog.low_btn.isSelected = true
            bottomSheetDialog.low_btn.alpha = 0.50f
        }
        bottomSheetDialog.medium_btn.setOnClickListener {
            resetSelectionPriorityLevel(bottomSheetDialog.low_btn)
            resetSelectionPriorityLevel(bottomSheetDialog.high_btn)
            bottomSheetDialog.medium_btn.isSelected = true
            bottomSheetDialog.medium_btn.alpha = 0.50f
        }
        bottomSheetDialog.high_btn.setOnClickListener {
            resetSelectionPriorityLevel(bottomSheetDialog.low_btn)
            resetSelectionPriorityLevel(bottomSheetDialog.medium_btn)
            bottomSheetDialog.high_btn.isSelected = true
            bottomSheetDialog.high_btn.alpha = 0.50f
        }
    }

    /**
     * Resets priority level button
     * @param priorityButton Button of bottom sheet dialog to be reset if been selected
     */
    private fun resetSelectionPriorityLevel(priorityButton: Button) {
        if (priorityButton.isSelected) {
            priorityButton.isSelected = false
            priorityButton.alpha = 1.0f
        }
    }
}