package com.azuresamples.msalandroidkotlinapp.singleaccountmode

import androidx.fragment.app.FragmentActivity

interface ParameterRequestObject {

    fun getActivity(): FragmentActivity

    fun getGraphUrl(): String
}