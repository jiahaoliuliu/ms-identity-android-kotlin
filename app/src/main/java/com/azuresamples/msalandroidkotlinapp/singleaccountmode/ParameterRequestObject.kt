package com.azuresamples.msalandroidkotlinapp.singleaccountmode

import androidx.fragment.app.FragmentActivity

interface ParameterRequestObject {

    fun getParentActivity(): FragmentActivity

    fun getScopes(): Array<String>

    fun getGraphUrl(): String
}