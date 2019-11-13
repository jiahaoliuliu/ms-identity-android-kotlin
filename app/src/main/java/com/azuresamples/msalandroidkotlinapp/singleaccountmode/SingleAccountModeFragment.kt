package com.azuresamples.msalandroidkotlinapp.singleaccountmode

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Response
import com.azuresamples.msalandroidkotlinapp.MSGraphRequestWrapper
import com.azuresamples.msalandroidkotlinapp.R
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import kotlinx.android.synthetic.main.fragment_single_account_mode.*
import org.json.JSONObject

class SingleAccountModeFragment : Fragment(), SingleAccountModeContract.View, ParameterRequestObject {
    companion object {

        private const val TAG = "SingleAccountMode"

        /* Azure AD v2 Configs */
        private const val AUTHORITY = "https://login.microsoftonline.com/common"
    }

    private lateinit var presenter: SingleAccountModeContract.Presenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_single_account_mode, container, false)
        presenter = SingleAccountModePresenter(this, this)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        presenter.onViewReady()
    }

    override fun createSingleAccountApplication() {
        // Creates a PublicClientApplication object with res/raw/auth_config_single_account.json
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context as Context,
            R.raw.auth_config_single_account,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    presenter.onSingleAccountApplicationCreationSuccess(application)
                }

                override fun onError(exception: MsalException) {
                    presenter.onSingleAccountApplicationCreationFailed(exception)
                }
            })
    }

    override fun showException(exception: Exception) {
        txt_log.text = exception.toString()
    }

    /**
     * Initializes UI variables and callbacks.
     */
    private fun initializeUI() {
        btn_signIn.setOnClickListener {
            presenter.onSignInRequested()
        }

        btn_removeAccount.setOnClickListener(View.OnClickListener {
            if (mSingleAccountApp == null) {
                return@OnClickListener
            }

            /**
             * Removes the signed-in account and cached tokens from this app.
             */
            mSingleAccountApp!!.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    updateUI(null)
                    performOperationOnSignOut()
                }

                override fun onError(exception: MsalException) {
                    showException(exception)
                }
            })
        })

        btn_callGraphInteractively.setOnClickListener(View.OnClickListener {
            if (mSingleAccountApp == null) {
                return@OnClickListener
            }

            /**
             * If acquireTokenSilent() returns an error that requires an interaction,
             * invoke acquireToken() to have the user resolve the interrupt interactively.
             *
             * Some example scenarios are
             * - password change
             * - the resource you're acquiring a token for has a stricter set of requirement than your SSO refresh token.
             * - you're introducing a new scope which the user has never consented for.
             */

            /**
             * If acquireTokenSilent() returns an error that requires an interaction,
             * invoke acquireToken() to have the user resolve the interrupt interactively.
             *
             * Some example scenarios are
             * - password change
             * - the resource you're acquiring a token for has a stricter set of requirement than your SSO refresh token.
             * - you're introducing a new scope which the user has never consented for.
             */
            mSingleAccountApp!!.acquireToken(activity!!, getScopes(), getAuthInteractiveCallback())
        })

        btn_callGraphSilently.setOnClickListener(View.OnClickListener {
            if (mSingleAccountApp == null) {
                return@OnClickListener
            }

            /**
             * Once you've signed the user in,
             * you can perform acquireTokenSilent to obtain resources without interrupting the user.
             */

            /**
             * Once you've signed the user in,
             * you can perform acquireTokenSilent to obtain resources without interrupting the user.
             */
            mSingleAccountApp!!.acquireTokenSilentAsync(getScopes(), AUTHORITY, getAuthSilentCallback())
        })

    }

    override fun onResume() {
        super.onResume()

        initializeUI()
        /**
         * The account may have been removed from the device (if broker is in use).
         * Therefore, we want to update the account state by invoking loadAccount() here.
         */
        loadAccount()
    }

    /**
     * Extracts a scope array from a text field,
     * i.e. from "User.Read User.ReadWrite" to ["user.read", "user.readwrite"]
     */
    override fun getScopes(): Array<String> {
        return scope.text.toString().toLowerCase().split(" ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
    }

    /**
     * Callback used in for silent acquireToken calls.
     * Looks if tokens are in the cache (refreshes if necessary and if we don't forceRefresh)
     * else errors that we need to do an interactive request.
     */
    private fun getAuthSilentCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {

            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                Log.d(TAG, "Successfully authenticated")

                /* Successfully got a token, use it to call a protected resource - MSGraph */
                callGraphAPI(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: $exception")
                showException(exception)

                if (exception is MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception is MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                } else if (exception is MsalUiRequiredException) {
                    /* Tokens expired or no session, retry with interactive */
                }
            }

            override fun onCancel() {
                /* User cancelled the authentication */
                Log.d(TAG, "User cancelled login.")
            }
        }
    }

    //
    // Helper methods manage UI updates
    // ================================
    // displayGraphResult() - Display the graph response
    // showException() - Display the graph response
    // updateSignedInUI() - Updates UI when the user is signed in
    // updateSignedOutUI() - Updates UI when app sign out succeeds
    //

    /**
     * Display the graph response
     */
    override fun displayGraphResult(graphResponse: JSONObject) {
        txt_log.text = graphResponse.toString()
    }

    override fun getGraphUrl() = msgraph_url.text.toString()

    /**
     * Updates UI based on the current account.
     */
    override fun updateUI(account: IAccount?) {

        if (account != null) {
            btn_signIn.isEnabled = false
            btn_removeAccount.isEnabled = true
            btn_callGraphInteractively.isEnabled = true
            btn_callGraphSilently.isEnabled = true
            current_user.text = account.username
        } else {
            btn_signIn.isEnabled = true
            btn_removeAccount.isEnabled = false
            btn_callGraphInteractively.isEnabled = false
            btn_callGraphSilently.isEnabled = false
            current_user.text = ""
        }
    }

    /**
     * Updates UI when app sign out succeeds
     */
    override fun showUserLoggedOut() {
        val signOutText = "Signed Out."
        current_user.text = ""
        Toast.makeText(context, signOutText, Toast.LENGTH_SHORT)
            .show()
    }
}