package com.azuresamples.msalandroidkotlinapp.singleaccountmode

import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import org.json.JSONObject

interface SingleAccountModeContract {

    interface View {
        /**
         * Create a simple client application
         */
        fun createSingleAccountApplication()

        fun showException(exception: Exception)

        fun updateUI(activeAccount: IAccount?)

        fun showUserLoggedOut()

        fun getScopes(): Array<String>

        fun displayGraphResult(graphResponse: JSONObject)
    }

    interface Presenter {

        /**
         * Notify that the view is ready
         */
        fun onViewReady()

        fun onSingleAccountApplicationCreationSuccess(application: ISingleAccountPublicClientApplication)

        fun onSingleAccountApplicationCreationFailed(exception: MsalException)

        fun onSignInRequested()
    }

}