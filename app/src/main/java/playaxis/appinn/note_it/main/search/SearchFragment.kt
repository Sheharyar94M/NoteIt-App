package playaxis.appinn.note_it.main.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.databinding.FragmentSearchBinding
import playaxis.appinn.note_it.extensions.showKeyboard
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.NoteAdapter
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import playaxis.appinn.note_it.main.viewModels.viewModel
import playaxis.appinn.note_it.preferences.PrefsManager
import javax.inject.Inject

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    @Inject
    lateinit var viewModelFactory: SearchViewModel.Factory
    private val viewModel by viewModel { viewModelFactory.create(it) }

    private val viewModelHome: HomeViewModel by activityViewModels()
    private val viewModelMain: MainViewModel by activityViewModels()

    private var currentHomeDestinationChanged: Boolean = false

    @Inject
    lateinit var prefsManager: PrefsManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSearchBinding.inflate(inflater,container,false)

        //Injection
        (requireContext().applicationContext as QuickNotepad).appComponent.inject(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recycler view
        val rcv = binding.notesList
        (rcv.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        //recyclerview
        rcv.setHasFixedSize(true)
        val adapter = NoteAdapter(requireContext(), viewModelHome, prefsManager)
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        rcv.adapter = adapter
        rcv.layoutManager = layoutManager
        rcv.itemAnimator = object : DefaultItemAnimator() {
            override fun animateAppearance(
                viewHolder: RecyclerView.ViewHolder,
                preLayoutInfo: ItemHolderInfo?,
                postLayoutInfo: ItemHolderInfo
            ): Boolean {
                return if (preLayoutInfo != null && (preLayoutInfo.left != postLayoutInfo.left
                            || preLayoutInfo.top != postLayoutInfo.top)
                ) {
                    // item move, handle normally
                    super.animateAppearance(viewHolder, preLayoutInfo, postLayoutInfo)
                } else {
                    // do not animate new item appearance
                    // this is mainly to avoid animating the whole list when fragment view is recreated.
                    dispatchAddFinished(viewHolder)
                    false
                }
            }
        }

        // Search view
        val searchView = binding.search
        //Perform search
        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(query: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchNotes(query.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        //handling the keyBoard view
        binding.search.onFocusChangeListener = View.OnFocusChangeListener { editText, hasFocus ->

            if (hasFocus) {
                editText.showKeyboard()
            }
        }

        searchView.requestFocus()

        //finishing the fragment
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        observers(adapter)
    }

    private fun observers(adapter: NoteAdapter){

        viewModel.listNotesLiveData.observe(viewLifecycleOwner) { items ->

            adapter.submitList(items, ::noteListCommitCallback)
        }

        viewModelMain.currentHomeDestination.observe(viewLifecycleOwner) {
            currentHomeDestinationChanged = true
        }
    }

    private fun noteListCommitCallback() {
        // Scroll to top of notes list, when the HomeDestination has changed
        if (currentHomeDestinationChanged) {
            if (binding.notesList.adapter!!.itemCount > 0) {
                binding.notesList.scrollToPosition(0)
                binding.notesList.scrollBy(0, -1)
            }
            currentHomeDestinationChanged = false
        }
    }

    override fun onDetach() {
        super.onDetach()

        viewModelMain.bottomFragmentItemDetachEventEvent()
    }
}
