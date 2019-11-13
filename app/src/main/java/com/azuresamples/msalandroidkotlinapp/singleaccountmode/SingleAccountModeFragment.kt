package com.azuresamples.msalandroidkotlinapp.singleaccountmode

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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

    override fun getParentActivity() = activity!!

    override fun showException(exception: Exception) {
        txt_log.text = exception.toString()
    }

    /**
     * Initializes UI variables and callbacks.
     */
    override fun initializeUI() {
        btn_signIn.setOnClickListener {
            presenter.onSignInRequested()
        }

        btn_removeAccount.setOnClickListener {
            presenter.onRemoveAccountRequested()
        }

        btn_callGraphInteractively.setOnClickListener {
            presenter.onCallGraphInteractivelyRequested()
        }

        btn_callGraphSilently.setOnClickListener(View.OnClickListener {
            presenter.onCallGraphSilentlyRequested()
        })
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }

    /**
     * Extracts a scope array from a text field,
     * i.e. from "User.Read User.ReadWrite" to ["user.read", "user.readwrite"]
     */
    override fun getScopes(): Array<String> {
        return scope.text.toString().toLowerCase().split(" ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
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