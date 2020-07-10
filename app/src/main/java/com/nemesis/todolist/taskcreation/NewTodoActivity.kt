package com.nemesis.todolist.taskcreation

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nemesis.todolist.R
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.todo_row.view.*


class NewTodoActivity(
    private val todo: String,
    private var isCompleted: Boolean,
    private var priority: String,
    private var creationDate: String,
    private var lastUpdated: String,
    private var documentId: String
) : Item<GroupieViewHolder>() {

    companion object {
        const val TAG = "NewTodoActivity"
        val db = Firebase.firestore
        val uid = FirebaseAuth.getInstance().uid ?: ""
    }

    var textTest = todo

    override fun getLayout(): Int {
        return R.layout.todo_row
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        // Set value of text view
        viewHolder.itemView.tv_list_name.text = todo
        // Set value of checkbox completed value
        viewHolder.itemView.todo_checkbox.isChecked = isCompleted

        // Delete todo
        viewHolder.itemView.btn_delete_todo.setOnClickListener {
            val db = Firebase.firestore
            val uid = FirebaseAuth.getInstance().uid ?: ""
            // Deleting document in which the item in the row belongs to by its Firestore document
            // id
            db.collection("todolist").document(uid).collection("todos")
                .document(documentId)
                .delete()
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully deleted!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error deleting document", e) }

            // Adjusting adapters and notifying for change
            CreateTodoActivity.adapter.removeGroupAtAdapterPosition(position)
            CreateTodoActivity.adapter.notifyItemChanged(position)
            CreateTodoActivity.adapter.notifyItemRangeChanged(
                position,
                CreateTodoActivity.adapter.itemCount
            )
        }
        viewHolder.itemView.todo_checkbox.setOnClickListener {
            if (lastUpdated != CreateTodoActivity.getDate()) {
                updateTodoCompletionStatus(isCompleted, viewHolder)
            } else {
                Toast.makeText(
                    CreateTodoActivity.getContext(),
                    "Been updated today",
                    Toast.LENGTH_SHORT
                ).show()
                viewHolder.itemView.todo_checkbox.isChecked = isCompleted
            }
        }

        // RGB values
        var red = 0
        var green = 0
        var blue = 0

        when (priority){
            "Low" -> {
                // Low RGB values
                red = 61
                green = 235
                blue = 52
            }
            "Medium" -> {
                // Medium RGB values
                red = 214
                green = 235
                blue = 52
            }
            "High" -> {
                // High RGB values
                red = 235
                green = 52
                blue = 52
            }
        }
        val colorStateList = ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_enabled)
            ),
            intArrayOf(Color.BLACK, Color.rgb(red,green,blue)))
        viewHolder.itemView.todo_checkbox.buttonTintList = colorStateList
    }

    /**
     * Updates the completion status of the todo within the todo list
     *
     * @param todoCompletion Boolean value of todos completion status
     * @param viewHolder GroupieViewHolder of the row within the todo list
     */
    private fun updateTodoCompletionStatus(todoCompletion: Boolean, viewHolder: GroupieViewHolder) {
        // Checkbox is checked
        viewHolder.itemView.todo_checkbox.isChecked = !isCompleted
        val update = hashMapOf(
            "creationDate" to creationDate,
            "isCompleted" to !todoCompletion,
            "priority" to priority,
            "todo" to todo,
            "updatedDate" to CreateTodoActivity.getDate()
        )
        db.collection("todolist").document(uid).collection("todos")
            .document(documentId)
            .set(update)
            .addOnSuccessListener { Log.d(TAG, "Update successful!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }
        isCompleted = !todoCompletion
        lastUpdated = CreateTodoActivity.getDate()
    }
}
