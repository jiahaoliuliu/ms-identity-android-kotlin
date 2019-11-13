package com.azuresamples.msalandroidkotlinapp.singleaccountmode

import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException

interface SingleAccountModeContract {

    interface View {
        /**
         * Create a simple client application
         */
        fun createSingleAccountApplication()

        fun showException(toString: String)
    }

    interface Presenter {

        /**
         * Notify that the view is ready
         */
        fun onViewReady()

        fun onSingleAccountApplicationCreationSuccess(application: ISingleAccountPublicClientApplication)

        fun onSingleAccountApplicationCreationFailed(exception: MsalException)
    }

}