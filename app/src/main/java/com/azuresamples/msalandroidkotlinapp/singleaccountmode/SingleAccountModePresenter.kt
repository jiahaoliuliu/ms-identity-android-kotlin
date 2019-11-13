package com.azuresamples.msalandroidkotlinapp.singleaccountmode

import android.util.Log
import com.android.volley.Response
import com.azuresamples.msalandroidkotlinapp.MSGraphRequestWrapper
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.microsoft.identity.client.exception.MsalUiRequiredException

class SingleAccountModePresenter(private val view: SingleAccountModeContract.View,
                                 private val parameterRequestObject: ParameterRequestObject): SingleAccountModeContract.Presenter {
    companion object {

        private const val TAG = "SingleAccountMode"

        /* Azure AD v2 Configs */
        private const val AUTHORITY = "https://login.microsoftonline.com/common"
    }

    /* Azure AD Variables */
    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null

    override fun onViewReady() {
        view.createSingleAccountApplication()
    }

    override fun onSingleAccountApplicationCreationSuccess(application: ISingleAccountPublicClientApplication) {
        /**
         * This test app assumes that the app is only going to support one account.
         * This requires "account_mode" : "SINGLE" in the config json file.
         *
         */
        mSingleAccountApp = application

        loadAccount()
    }

    /**
     * Load the currently signed-in account, if there's any.
     * If the account is removed the device, the app can also perform the clean-up work in onAccountChanged().
     */
    private fun loadAccount() {
        if (mSingleAccountApp == null) {
            return
        }

        mSingleAccountApp!!.getCurrentAccountAsync(object :
            ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                view.updateUI(activeAccount)
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                if (currentAccount == null) {
                    // Perform a cleanup task as the signed-in account changed.
                    view.showUserLoggedOut()
                }
            }

            override fun onError(exception: MsalException) {
                view.showException(exception)
            }
        })
    }

    override fun onSignInRequested() {
        if (mSingleAccountApp == null) {
            return
        }

        mSingleAccountApp!!.signIn(
            parameterRequestObject.getParentActivity(),
            "",
            parameterRequestObject.getScopes(), getAuthInteractiveCallback())
    }

    override fun onSingleAccountApplicationCreationFailed(exception: MsalException) {
        view.showException(exception)
    }

    override fun onViewResumed() {
        view.initializeUI()
        /**
         * The account may have been removed from the device (if broker is in use).
         * Therefore, we want to update the account state by invoking loadAccount() here.
         */
        loadAccount()
    }

    override fun onRemoveAccountRequested() {

        if (mSingleAccountApp == null) {
            return
        }

        /**
         * Removes the signed-in account and cached tokens from this app.
         */
        mSingleAccountApp!!.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                view.updateUI(null)
                view.showUserLoggedOut()
            }

            override fun onError(exception: MsalException) {
                view.showException(exception)
            }
        })
    }

    override fun onCallGraphInteractivelyRequested() {
        if (mSingleAccountApp == null) {
            return
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
        mSingleAccountApp!!.acquireToken(
            parameterRequestObject.getParentActivity(),
            parameterRequestObject.getScopes(), getAuthInteractiveCallback())
    }

    override fun onCallGraphSilentlyRequested() {
        if (mSingleAccountApp == null) {
            return
        }

        /**
         * Once you've signed the user in,
         * you can perform acquireTokenSilent to obtain resources without interrupting the user.
         */

        /**
         * Once you've signed the user in,
         * you can perform acquireTokenSilent to obtain resources without interrupting the user.
         */
        mSingleAccountApp!!.acquireTokenSilentAsync(parameterRequestObject.getScopes(),
            AUTHORITY, getAuthSilentCallback())
    }

    /**
     * Callback used for interactive request.
     * If succeeds we use the access token to call the Microsoft Graph.
     * Does not check cache.
     */
    private fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {

            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                /* Successfully got a token, use it to call a protected resource - MSGraph */
                Log.d(TAG, "Successfully authenticated")
                Log.d(TAG, "ID Token: " + authenticationResult.account.claims!!["id_token"])

                /* Update account */
                view.updateUI(authenticationResult.account)

                /* call graph */
                callGraphAPI(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: $exception")
                view.showException(exception)

                if (exception is MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception is MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                }
            }

            override fun onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.")
            }
        }
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
                view.showException(exception)

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

    /**
     * Make an HTTP request to obtain MSGraph data
     */
    private fun callGraphAPI(authenticationResult: IAuthenticationResult) {
        MSGraphRequestWrapper.callGraphAPIWithVolley(parameterRequestObject.getParentActivity(),
            parameterRequestObject.getGraphUrl(),
            authenticationResult.accessToken,
            Response.Listener { response ->
                /* Successfully called graph, process data and send to UI */
                Log.d(TAG, "Response: $response")
                view.displayGraphResult(response)
            },
            Response.ErrorListener { error ->
                Log.d(TAG, "Error: $error")
                view.showException(error)
            })
    }
}