package com.nemesis.todolist.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignIn.getClient
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder
import com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.nemesis.todolist.R
import com.nemesis.todolist.taskcreation.CreateTodoActivity
import kotlinx.android.synthetic.main.activity_google_sign_in.*


class LoginActivity : AppCompatActivity() {

    companion object {
        fun getLaunchIntent(from: Context) = Intent(from, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        const val TAG = "LoginActivity"
    }

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mGoogleSignInOptions: GoogleSignInOptions
    private val RC_SIGN_IN: Int = 1
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_sign_in)
        firebaseAuth = FirebaseAuth.getInstance()
        configureGoogleSignIn()
        setupUI()
    }

    /**
     * Configures the sign in to request users IdToken and their email
     */
    private fun configureGoogleSignIn() {
        mGoogleSignInOptions = Builder(DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = getClient(this, mGoogleSignInOptions)
    }

    private fun setupUI() {
        btn_sign_in.setOnClickListener {
            btn_sign_in.isEnabled = false
            signIn()
        }
    }

    /**
     * Prompts option for user to enter their Google account credentials
     */
    private fun signIn() {
        // User is prompted to select a Google account to sign in with
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Sign in has failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Authorises the users Google Account with Firebase
     * @param acct GoogleSignInAccount is the users Google Account
     */
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                startActivity(CreateTodoActivity.getLaunchIntent(this))
            } else {
                Toast.makeText(this, "Sign in has failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            startActivity(CreateTodoActivity.getLaunchIntent(this))
            finish()
        }
    }
}