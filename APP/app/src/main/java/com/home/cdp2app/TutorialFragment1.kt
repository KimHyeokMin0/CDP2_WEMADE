package com.home.cdp2app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.home.cdp2app.databinding.FragmentTutorial1Binding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TutorialFragment1.newInstance] factory method to
 * create an instance of this fragment.
 */
class TutorialFragment1 : Fragment() {
    // 튜토리얼 항목 1
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentTutorial1Binding.inflate(inflater, container, false).root
    }
}