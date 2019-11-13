package com.azuresamples.msalandroidkotlinapp.singleaccountmode

import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException

class SingleAccountModePresenter(private val view: SingleAccountModeContract.View): SingleAccountModeContract.Presenter {

    /* Azure AD Variables */
    private var singleAccountApp: ISingleAccountPublicClientApplication? = null

    override fun onViewReady() {
        view.createSingleAccountApplication()
        // Creates a PublicClientApplication object with res/raw/auth_config_single_account.json
//        PublicClientApplication.createSingleAccountPublicClientApplication(
//            context as Context,
//            R.raw.auth_config_single_account,
//            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
//                override fun onCreated(application: ISingleAccountPublicClientApplication) {
//                    /**
//                     * This test app assumes that the app is only going to support one account.
//                     * This requires "account_mode" : "SINGLE" in the config json file.
//                     *
//                     */
//                    mSingleAccountApp = application
//
//                    loadAccount()
//                }
//
//                override fun onError(exception: MsalException) {
//                    txt_log.text = exception.toString()
//                }
//            })
    }

    override fun onSingleAccountApplicationCreationSuccess(application: ISingleAccountPublicClientApplication) {
        /**
         * This test app assumes that the app is only going to support one account.
         * This requires "account_mode" : "SINGLE" in the config json file.
         *
         */
        singleAccountApp = application

        loadAccount()
    }

    override fun onSingleAccountApplicationCreationFailed(exception: MsalException) {
        view.showException(exception.toString())
    }

}