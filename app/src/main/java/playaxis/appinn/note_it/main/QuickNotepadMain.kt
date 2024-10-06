package playaxis.appinn.note_it.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.contains
import androidx.core.view.forEach
import androidx.core.view.iterator
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import playaxis.appinn.note_it.HomeNavGraphDirections
import playaxis.appinn.note_it.MainNavigationDirections
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.databinding.QuickNotepadMainBinding
import playaxis.appinn.note_it.extensions.navigateSafe
import playaxis.appinn.note_it.main.fragment_home.HomeFragmentDirections
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.fragment_home.account.sign_in.SignInDialogFragment
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.AddEditNoteBottomSheetDialog
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.dialogs.AddEditNoteBottomSheetDialog.Companion
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.DrawFragmentDirections
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.note_lock.lock.NoteCreateLockDialogFragment
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.note_lock.unlock.NoteUnlockDialogFragment
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.viewmodels.AddEditNoteViewModel
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.viewmodels.utils.EditMessage
import playaxis.appinn.note_it.main.fragment_home.fragments.note.NoteViewModel
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteItem
import playaxis.appinn.note_it.main.fragment_home.fragments.note.adapter.utils.NoteListLayoutMode
import playaxis.appinn.note_it.main.utils.observeEvent
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import playaxis.appinn.note_it.main.viewModels.viewModel
import playaxis.appinn.note_it.navigation.HomeDestination
import playaxis.appinn.note_it.preferences.SharedPreference
import playaxis.appinn.note_it.receiver.AlarmReceiver
import playaxis.appinn.note_it.repository.model.converter.NoteTypeConverter
import playaxis.appinn.note_it.repository.model.entities.Note
import playaxis.appinn.note_it.repository.model.entities.NoteStatus
import playaxis.appinn.note_it.repository.model.entities.NoteType
import playaxis.appinn.note_it.repository.model.entities.PinnedStatus
import playaxis.appinn.note_it.utils.MainUtils
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject

class QuickNotepadMain : AppCompatActivity(), SignInDialogFragment.SignInEvents, NavController.OnDestinationChangedListener,
    NoteUnlockDialogFragment.CloseUnLockerEvent, NoteCreateLockDialogFragment.CloseLockerEvent, NoteCreateLockDialogFragment.AddLockToNoteEvent {

    private lateinit var binding: QuickNotepadMainBinding

    @Inject
    lateinit var viewModelFactory: MainViewModel.Factory
    private val viewModelMain by viewModel { viewModelFactory.create(it) }

    @Inject
    lateinit var viewModelHomeFactory: HomeViewModel.Factory
    private val viewModelHome by viewModel { viewModelHomeFactory.create(it) }

    @Inject
    lateinit var viewModelAddEditFactory: AddEditNoteViewModel.Factory
    private val viewModelAddEdit by viewModel { viewModelAddEditFactory.create(it) }

    //fragment objects
    private lateinit var signin: SignInDialogFragment
    private lateinit var navController: NavController

    private lateinit var unLockNoteDialog: NoteUnlockDialogFragment
    private lateinit var createLockNoteDialog: NoteCreateLockDialogFragment

    //delete dialog
    private lateinit var dialogDelete: Dialog
    private val PERMISSION_REQUEST_CODE = 1
    private var REQUEST_CODE_STORAGE = 3
    private val REQ_CODE_CAMERA = 100

    private val restoreNoteSnackbar by lazy {
        Snackbar.make(binding.root, R.string.edit_in_trash_message,
            CANT_EDIT_SNACKBAR_DURATION)
            .setGestureInsetBottomIgnored(true)
            .setAction(R.string.restore) { viewModelAddEdit.restoreNoteAndEdit(viewModelHome.getClickedNote()) }
    }

    //current destination
    private var currentHomeDestination: HomeDestination = HomeDestination.Status(NoteStatus.ACTIVE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = QuickNotepadMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //injection initialization
        (QuickNotepad.appContext as QuickNotepad).appComponent.inject(this)

        //dialog of delete item from trash
        dialogDelete = Dialog(this)


        //unLock Note Dialog
        unLockNoteDialog = NoteUnlockDialogFragment(this)
        //Lock Note Dialog
        createLockNoteDialog = NoteCreateLockDialogFragment(this, this)

        //status bar color
        MainUtils.statusBarColor(this@QuickNotepadMain, ContextCompat.getColor(this, R.color.theme_color))

        //nullifying default icon tints
        binding.drawerNav.itemIconTintList = null

        //sign-in fragment
        signin = SignInDialogFragment(this)

        val host = supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
        navController = host.navController
        navController.setGraph(R.navigation.main_navigation)
        navController.addOnDestinationChangedListener(this)

        // Apply padding to navigation drawer
        val initialPadding = resources.getDimensionPixelSize(R.dimen.navigation_drawer_bottom_padding)
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerNav) { _, insets ->
            val sysWindow = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.drawerNav.getHeaderView(0).updatePadding(top = sysWindow.top)
            binding.drawerNav.children.last()
                .updatePadding(bottom = initialPadding + sysWindow.bottom)
            // Don't draw under system bars, if it conflicts with the navigation drawer.
            // This is mainly the case if the app is used in landscape mode with traditional 3 button navigation.

            if (sysWindow.left > 0) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
            insets
        }

        binding.drawerNav.setNavigationItemSelectedListener { item ->

            val itemMenu = binding.drawerNav.menu.findItem(R.id.drawer_labels)
            viewModelMain.navigationItemSelected(item, itemMenu.subMenu!!)
            true
        }
        viewModelMain.startPopulatingDrawerWithLabels()

        onBackPressedDispatcher.addCallback(this) {

            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawers()
            }
            else {
                // The dispatcher only calls the topmost enabled callback, so temporarily
                // disable it to be able to call the next callback on the stack.
                isEnabled = false

                //current fragment
                val currentFragId = navController.currentBackStackEntry!!.destination.id
                val currentFragment = navController.graph.findNode(currentFragId)

                if (currentFragment!!.id != R.id.homeFragment || !viewModelHome.tabSelected) {

                    if (currentFragment.id == R.id.addEditFragment ||
                        currentFragment.id == R.id.todoFragment ||
                        currentFragment.id == R.id.speechFragment) {

                        //fire the event and save the data
                        viewModelMain.saveNoteOnBackPressEvent()

                        val previousFragId = navController.previousBackStackEntry!!.destination.id
                        val previousFragment = navController.graph.findNode(previousFragId)

                        //popping stack
                        if (previousFragment!!.label == "fragment_draw") {
                            navController.popBackStack()
                            navController.popBackStack()
                        } else
                            navController.popBackStack()
                    }
                    else if (currentFragment.label == "fragment_draw") {
                        //visibility set
                        viewModelMain.bottomBarItemsListenerEvent("add_note")
                        binding.toolbarLayout.toolbarLayout.visibility = View.VISIBLE
                        binding.searchBarView.searchBarView.visibility = View.GONE

                        navController.navigateSafe(DrawFragmentDirections.actionDrawFragmentToAddEditFragment())
                    }
                    else {
                        //visibility set
                        binding.toolbarLayout.toolbarLayout.visibility = View.GONE
                        binding.searchBarView.searchBarView.visibility = View.VISIBLE
                        //back pressed
                        navController.popBackStack()
                    }
                }
                else if (viewModelHome.tabSelected) {
                    //visibility set
                    binding.toolbarLayout.toolbarLayout.visibility = View.GONE
                    binding.searchBarView.searchBarView.visibility = View.VISIBLE

                    //sending event
                    viewModelMain.onBackPressed()
                    isEnabled = true
                }
            }
        }

        //search bar listeners
        searchBarListeners()
        mainObservers()
        toolbarListeners()
    }

    private fun updateItemsForSelection(selection: NoteViewModel.NoteSelection) {

        // Pin item
        val pinItem = binding.toolbarLayout.pinButton
        when (selection.pinned) {
            PinnedStatus.PINNED -> {
                pinItem.visibility = View.VISIBLE
                pinItem.isEnabled = true
                pinItem.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pinned_icon))
            }

            PinnedStatus.UNPINNED -> {
                pinItem.visibility = View.VISIBLE
                pinItem.isEnabled = true
                pinItem.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.unpin_icon))
            }

            PinnedStatus.CANT_PIN -> {
                pinItem.visibility = View.GONE
                pinItem.isEnabled = false
            }
        }

        // Reminder item
        val reminderItem = binding.toolbarLayout.reminderButton
        if (selection.hasReminder && selection.status == NoteStatus.ACTIVE)
            reminderItem.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.reminder_icon_
                )
            )
        else
            reminderItem.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.reminder_icon_
                )
            )

        //archive item
        val archiveItem = binding.toolbarLayout.archiveButton
        if (selection.status == NoteStatus.ARCHIVED)
            archiveItem.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.un_archive))
        else
            archiveItem.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.archive))

        //lock remove password
        val lockItem = binding.toolbarLayout.lockButton
        if (selection.isLocked) {
            unLockNoteDialog.showRemoveLockDialog = true
            lockItem.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.lock_remove))
        }
        else {
            unLockNoteDialog.showRemoveLockDialog = false
            lockItem.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.lock))
        }
    }

    private fun toolbarListeners() {

        /**
        Saving Note and getting data from HomeViewModel and setting the values to AddEditViewModel
         **/
        binding.toolbarLayout.backButton.setOnClickListener {

            //previous
            if (navController.currentBackStackEntry!!.destination.id != R.id.homeFragment) {

                if (navController.previousBackStackEntry != null) {

                    val previousFragId = navController.previousBackStackEntry!!.destination.id
                    val previousFragment = navController.graph.findNode(previousFragId)

                    //current fragment
                    val currentFragId = navController.currentBackStackEntry!!.destination.id
                    val currentFragment = navController.graph.findNode(currentFragId)

                    if (currentFragment!!.id == R.id.addEditFragment || currentFragment.id == R.id.todoFragment || currentFragment.id == R.id.speechFragment) {

                        //fire the event and save the data
                        viewModelMain.saveNoteOnBackPressEvent()

                        //popping stack
                        if (previousFragment!!.label == "fragment_draw") {
                            navController.popBackStack()
                            navController.popBackStack()
                        }
                        else if (previousFragment.label == "fragment_camera"){
                            navController.popBackStack()
                            navController.popBackStack()
                        }
                        else if (previousFragment.label == "fragment_add_edit" || previousFragment.label == "fragment_todo") {
                            navController.popBackStack()
                        }
                        else {
                            if (currentFragment.label == "fragment_add_edit") {

                                navController.popBackStack()
                                viewModelHome.clearSelection()
                                Handler().postDelayed({
                                    //to normalize the view
                                    viewModelMain.bottomFragmentItemDetachEventEvent()
                                }, 50)
                            }
                            else{
                                navController.popBackStack()
                                viewModelHome.clearSelection()
                                viewModelHome.selectedNoteIds.clear()
                            }
                        }
                    }
                    else if (currentFragment.label == "fragment_draw") {

                        //visibility set
                        viewModelMain.bottomBarItemsListenerEvent("add_note")
                        binding.toolbarLayout.toolbarLayout.visibility = View.VISIBLE
                        binding.searchBarView.searchBarView.visibility = View.GONE

                        lifecycleScope.launch(Dispatchers.IO) {
                            runOnUiThread {
                                navController.navigateSafe(DrawFragmentDirections.actionDrawFragmentToAddEditFragment())
                            }
                        }
                    }
                    else if (currentFragment.label == "CreateEditLabelFragment") {

                        //save label
                        viewModelHome.saveLabelEvent()
                        navController.popBackStack()
                        //adjust the view
                        if (previousFragment!!.label != "fragment_home"){
                            binding.toolbarLayout.toolbarLayout.visibility = View.VISIBLE
                            binding.searchBarView.searchBarView.visibility = View.GONE
                        }
                    }
                    else if (currentFragment.label == "fragment_archive") {

                        binding.searchBarView.searchBarView.visibility = View.VISIBLE
                        binding.toolbarLayout.toolbarLayout.visibility = View.GONE
                        //clear selection
                        viewModelHome.clearSelection()
                        viewModelHome.selectedNoteIds.clear()
                    }
                    else if (currentFragment.label == "fragment_trash") {

                        binding.searchBarView.searchBarView.visibility = View.VISIBLE
                        binding.toolbarLayout.toolbarLayout.visibility = View.GONE
                        //clear selection
                        viewModelHome.clearSelection()
                        viewModelHome.selectedNoteIds.clear()
                    }
                    else if (currentFragment.label == "fragment_camera") {
                        navController.popBackStack()

                        if (previousFragment!!.label == "fragment_home") {
                            Handler().postDelayed({
                                //to normalize the view
                                viewModelMain.bottomFragmentItemDetachEventEvent()
                            }, 100)
                        }
                    }
                    else {
                        navController.popBackStack()
                        viewModelHome.clearSelection()
                        viewModelHome.selectedNoteIds.clear()
                    }
                }
                else {

                    navController.setGraph(R.navigation.main_navigation)
                    viewModelHome.clearSelection()
                    viewModelHome.selectedNoteIds.clear()
                    viewModelAddEdit.exit()
                }
            }
            else {

                if (viewModelHome.adapter.selectionFolder) {
                    //when fragment in main container remains home fragment (means that if NoteListFragment or FolderListFragment are added to the stack) then the control comes here!
                    viewModelHome.backPressOfHomeChildFragments()

                    //make toolbar invisible and search bar visible
                    binding.toolbarLayout.toolbarLayout.visibility = View.VISIBLE
                    binding.searchBarView.searchBarView.visibility = View.GONE
                    //make the toolbar of homepage
                    normalizeToolbarHomePage()
                    //updating select folder value (because user is no more in selection view!)
                    viewModelHome.adapter.selectionFolder = false
                }
                else {

                    //make toolbar invisible and search bar visible
                    binding.toolbarLayout.toolbarLayout.visibility = View.GONE
                    binding.searchBarView.searchBarView.visibility = View.VISIBLE

                    //clearing the selection
                    viewModelHome.clearSelection()
                }
            }
        }

        binding.toolbarLayout.pinButton.setOnClickListener {

            //pin the note
            viewModelHome.togglePin()
            //current fragment
            val currentFragId = navController.currentBackStackEntry!!.destination.id
            if(currentFragId == R.id.homeFragment || currentFragId == R.id.archiveFragment || currentFragId == R.id.trashFragment){
                //clearing the selection
                viewModelMain.bottomFragmentItemDetachEventEvent()
                viewModelHome.clearSelection()
            }
        }

        binding.toolbarLayout.reminderButton.setOnClickListener {

            //add reminder to note
            viewModelHome.createReminder()
            //current fragment
            val currentFragId = navController.currentBackStackEntry!!.destination.id
            if(currentFragId == R.id.homeFragment || currentFragId == R.id.archiveFragment || currentFragId == R.id.trashFragment){
                //clearing the selection
                viewModelMain.bottomFragmentItemDetachEventEvent()
                viewModelHome.clearSelection()
            }
        }

        binding.toolbarLayout.archiveButton.setOnClickListener {
            //setting note to archive
            viewModelHome.archiveNotes()
            viewModelHome.changeSelection = false

            //current fragment
            val currentFragId = navController.currentBackStackEntry!!.destination.id
            if(currentFragId == R.id.homeFragment || currentFragId == R.id.archiveFragment || currentFragId == R.id.trashFragment){
                viewModelHome.changeSelection = true
                //clearing the selection
                viewModelMain.bottomFragmentItemDetachEventEvent()
                viewModelHome.clearSelection()
            }
        }

        binding.toolbarLayout.labelButton.setOnClickListener {

            //sending the selected notes ids to the labels screen so that labels should be attached to the respective notes
            val selectedNotesIds = viewModelHome.selectedNotes.map { it.id }.toLongArray()
            navController.navigateSafe(MainNavigationDirections.mainToCreateEditLabelFragment(selectedNotesIds))
        }

        binding.toolbarLayout.lockButton.setOnClickListener {

            val note = viewModelHome.getClickedNote()
            //checking that the selected note has lock applied or not
            if (note.lock.isNotEmpty()) {

                //show remove lock dialog
                unLockNoteDialog.isCancelable = false
                unLockNoteDialog.show(supportFragmentManager, createLockNoteDialog.tag)
            }
            else {

                //show create lock dialog
                createLockNoteDialog.isCancelable = false
                createLockNoteDialog.show(supportFragmentManager, createLockNoteDialog.tag)
            }
        }

        binding.toolbarLayout.menuIcon.setOnClickListener {
            //show menu
            showMenu(it)
        }

        binding.toolbarLayout.moveToFolderButtonAction.setOnClickListener {
            viewModelHome.setMoveToFolderActionEvent()
        }

        binding.toolbarLayout.restoreAction.setOnClickListener {
            //restoring the item
            viewModelHome.restoreNoteFromTrash()
            //clearing selection after performing the operation
            viewModelMain.bottomFragmentItemDetachEventEvent()
            viewModelHome.clearSelection()
        }

        binding.toolbarLayout.deleteAction.setOnClickListener {
            //show the delete dialog and then delete on the positive button
            if (currentHomeDestination == HomeDestination.Status(NoteStatus.DELETED)){
                //show the dialog and then do this
                showDeleteDialog(false)
            }
            else{

                viewModelHome.deleteSelectedNotesPre()
                //clearing the selection
                viewModelMain.bottomFragmentItemDetachEventEvent()
                viewModelHome.clearSelection()
            }
        }

        binding.toolbarLayout.toolbarLayout.setOnClickListener {

            //fire an event to make font view invisible
            viewModelHome.toolbarListenerEvent()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showDeleteDialog(emptyTrash:Boolean) {

        //delete dialog here
        dialogDelete.setContentView(R.layout.delete_confirm_dialog)
        dialogDelete.let {

            val windowManager = windowManager
            val display = windowManager.defaultDisplay
            val size = android.graphics.Point()
            display.getSize(size)
            val screenWidth = size.x
            val screenHeight = size.y

            val dialogWidth = (screenWidth * 1.0).toInt()
            val dialogHeight = (screenHeight * 0.4).toInt()

            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialogDelete.window?.attributes)
            layoutParams.width = dialogWidth
            layoutParams.height = dialogHeight
            dialogDelete.window?.attributes = layoutParams
            dialogDelete.window?.setGravity(Gravity.CENTER)
            dialogDelete.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialogDelete.setCancelable(false)
        }

        // Find views in the custom layout
        val heading: AppCompatTextView = dialogDelete.findViewById(R.id.heading_delete_dialog)
        val emptyText: AppCompatTextView = dialogDelete.findViewById(R.id.text_confirmation)
        val cancel: AppCompatTextView = dialogDelete.findViewById(R.id.cancel_button)
        val delete: AppCompatTextView = dialogDelete.findViewById(R.id.delete_button)

        if (emptyTrash){
            heading.text = "Empty Trash"
            emptyText.text = "Are you sure you want to delete all items permanently?"
        }
        else{
            heading.text = "Delete Note"
            emptyText.text = "Are you sure you want to delete this item permanently?"
        }

        cancel.setOnClickListener {
            dialogDelete.dismiss()
        }

        delete.setOnClickListener {

            if (emptyTrash){
                //deleting all items
                viewModelHome.emptyTrash()
            }
            else{
                viewModelHome.deleteSelectedNotes()
                viewModelMain.bottomFragmentItemDetachEventEvent()
                viewModelHome.clearSelection()
            }

            dialogDelete.dismiss()
        }

        dialogDelete.show()
    }

    private fun showMenu(view: View) {
        val popupMenu = PopupMenu(this, view, 0, 0, R.style.CustomPopupMenu)

        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.home_menu, popupMenu.menu)

        //setting color of the text items
        for (item in popupMenu.menu.iterator()) {

            val spannable = SpannableString(item.title.toString())
            spannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        this,
                        R.color.dialog_text_color
                    )
                ), 0, spannable.length, 0
            )
            item.title = spannable
        }

        // Optionally, set a listener for menu item clicks
        popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem): Boolean {
                // Handle menu item clicks here
                when (item.itemId) {
                    R.id.archive -> {
                        //setting note to archive
                        viewModelHome.archiveNotes()
                        viewModelMain.bottomFragmentItemDetachEventEvent()
                        return true
                    }

                    R.id.delete -> {
                        viewModelHome.deleteSelectedNotesPre()
                        viewModelMain.bottomFragmentItemDetachEventEvent()
                        return true
                    }

                    R.id.make_copy -> {
                        viewModelHome.copySelectedNote(
                            getString(R.string.edit_copy_untitled_name),
                            getString(R.string.edit_copy_suffix)
                        )
                        viewModelMain.bottomFragmentItemDetachEventEvent()
                        return true
                    }

                    R.id.send -> {
                        viewModelHome.shareSelectedNote()
                        viewModelMain.bottomFragmentItemDetachEventEvent()
                        viewModelHome.clearSelection()
                        return true
                    }

                    R.id.move_to_folder -> {
                        //normalize the toolbar here according to the view of selection
                        normalizeToolbarForFolderSelection()
                        //now send the event to navigate
                        viewModelHome.setMoveToFolderSelectionEvent("")
                        return true
                    }
                }
                return false
            }
        })
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }

    private fun showMenuSearchBar(view: View) {
        val popupMenu = PopupMenu(this, view, 0, 0, R.style.CustomPopupMenu)

        if (currentHomeDestination == HomeDestination.Status(NoteStatus.DELETED)) {

            val inflater = popupMenu.menuInflater
            inflater.inflate(R.menu.trash_menu, popupMenu.menu)

            //setting color of the text items
            for (item in popupMenu.menu.iterator()) {

                val spannable = SpannableString(item.title.toString())
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.dialog_text_color)),
                    0, spannable.length, 0
                )

                item.title = spannable
            }

            // Optionally, set a listener for menu item clicks
            popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    // Handle menu item clicks here
                    when (item.itemId) {
                        R.id.restore_button -> {

                            //add all items to selection list
                            viewModelHome.addAllItemsToSelection()
                            //restore all items from the list
                            viewModelHome.restoreNoteFromTrash()
                            viewModelMain.bottomFragmentItemDetachEventEvent()
                            return true
                        }

                        R.id.empty_bin_button -> {

                            //delete all items from the list
                            viewModelHome.emptyTrashDialogEvent()
                            return true
                        }
                    }
                    return false
                }
            })
            popupMenu.setForceShowIcon(true)
            popupMenu.show()
        }
        else if (currentHomeDestination == HomeDestination.Status(NoteStatus.ARCHIVED)) {

            val inflater = popupMenu.menuInflater
            inflater.inflate(R.menu.archive_menu, popupMenu.menu)

            //setting color of the text items
            for (item in popupMenu.menu.iterator()) {

                val spannable = SpannableString(item.title.toString())
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.dialog_text_color)),
                    0, spannable.length, 0
                )

                item.title = spannable
            }

            // Optionally, set a listener for menu item clicks
            popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    // Handle menu item clicks here
                    when (item.itemId) {
                        R.id.unarchive_all_button -> {

                            //add all items to selection list
                            viewModelHome.addAllItemsToSelection()
                            //unarchive all items in the list
                            viewModelHome.unArchiveAll()
                            viewModelMain.bottomFragmentItemDetachEventEvent()
                            return true
                        }

                        R.id.delete_all_button -> {

                            //add all items to selection list
                            viewModelHome.addAllItemsToSelection()
                            //delete all items in the list
                            viewModelHome.deleteSelectedNotesPre()
                            viewModelMain.bottomFragmentItemDetachEventEvent()
                            return true
                        }
                    }
                    return false
                }
            })
            popupMenu.setForceShowIcon(true)
            popupMenu.show()
        }
    }

    private fun normalizeToolbarForFolderSelection() {

        binding.toolbarLayout.backButton.visibility = View.VISIBLE
        binding.toolbarLayout.moveToFolderButtonAction.visibility = View.VISIBLE
        binding.toolbarLayout.menuIcon.visibility = View.GONE
        binding.toolbarLayout.lockButton.visibility = View.GONE
        binding.toolbarLayout.labelButton.visibility = View.GONE
        binding.toolbarLayout.undoRedo.visibility = View.GONE
        binding.toolbarLayout.archiveButton.visibility = View.GONE
        binding.toolbarLayout.colorButton.visibility = View.GONE
        binding.toolbarLayout.pinButton.visibility = View.GONE
        binding.toolbarLayout.reminderButton.visibility = View.GONE
    }

    private fun searchBarListeners() {

        //show drawer
        binding.searchBarView.menuButton.setOnClickListener {

            //open menu drawer
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        //layout change list
        binding.searchBarView.layoutChangeButton.setOnClickListener {

            //send the event to the home fragment to make the list visible in the specific orientation
            viewModelHome.toggleListLayoutMode()
        }

        //account button click listener
        binding.searchBarView.accountButton.setOnClickListener {

            //show the dialog of the account
            if (FirebaseAuth.getInstance().currentUser != null)
                showAccountSignedUserInDialog()
            else
                showAccountSignInDialog()
        }

        //search button
        binding.searchBarView.searchButton.setOnClickListener {
            //call the search fragment
            navController.navigateSafe(HomeFragmentDirections.actionHomeFragmentToSearchFragment())
            //make view normal
            binding.toolbarLayout.toolbarLayout.visibility = View.GONE
            binding.searchBarView.searchBarView.visibility = View.GONE
        }

        //menu dots button
        binding.searchBarView.menuDotsButton.setOnClickListener {
            //show the menu for archived
            showMenuSearchBar(it)
        }
    }

    @SuppressLint("ShowToast")
    private fun mainObservers() {

        val menu = binding.drawerNav.menu
        val labelSubmenu = menu.findItem(R.id.drawer_labels).subMenu!!

        //to know which screen is called and set their data list respectively
        viewModelMain.currentHomeDestination.observe(this) { newHomeDestination ->
            viewModelHome.setDestination(newHomeDestination)
            currentHomeDestination = newHomeDestination

            //setting the list layout change button visibility handle, because it should be visible on every destination except folders
            binding.searchBarView.layoutChangeButton.visibility = View.VISIBLE

            //show menu for the archive and trash
            when (newHomeDestination) {
                HomeDestination.Status(NoteStatus.ARCHIVED), HomeDestination.Status(NoteStatus.DELETED) -> {

                    //show the menu icon and others invisible
                    binding.searchBarView.menuDotsButton.visibility = View.VISIBLE
                    binding.searchBarView.layoutChangeButton.visibility = View.GONE
                    binding.searchBarView.accountButton.visibility = View.GONE
                }

                HomeDestination.Reminders -> {
                    binding.searchBarView.menuDotsButton.visibility = View.GONE
                    binding.searchBarView.layoutChangeButton.visibility = View.GONE
                    binding.searchBarView.accountButton.visibility = View.GONE
                }
                else -> {

                    //invisible the menu icon and others visible
                    binding.searchBarView.menuDotsButton.visibility = View.GONE
                    binding.searchBarView.layoutChangeButton.visibility = View.VISIBLE
                    binding.searchBarView.accountButton.visibility = View.VISIBLE
                }
            }
        }

        viewModelHome.currentSelection.observe(this) { selection ->
            updateItemsForSelection(selection)
        }

        //'clearLabelsEvent' and 'labelsAddEvent' are updating the list of labels in drawer
        //for clearing labels
        viewModelMain.clearLabelsEvent.observeEvent(this) {
            labelSubmenu.clear()
        }

        //for adding labels
        viewModelMain.labelsAddEvent.observeEvent(this) { labels ->

            if (labels != null) {
                for (label in labels) {
                    labelSubmenu.add(Menu.NONE, View.generateViewId(), Menu.NONE, label.name)
                        .setIcon(R.drawable.label).isCheckable = true
                }
            }

            // Select the current label in the navigation drawer, if it isn't already.
            if (currentHomeDestination is HomeDestination.Labels) {
                val currentLabelName = (currentHomeDestination as HomeDestination.Labels).label.name
                if (binding.drawerNav.checkedItem != null && (binding.drawerNav.checkedItem!! !in labelSubmenu
                            || binding.drawerNav.checkedItem!!.title != currentLabelName) || binding.drawerNav.checkedItem == null
                ) {
                    labelSubmenu.forEach { item: MenuItem ->
                        if (item.title == currentLabelName) {
                            binding.drawerNav.setCheckedItem(item)
                            return@forEach
                        }
                    }
                }
            }
        }

        //navigation of drawer
        viewModelMain.navDirectionsEvent.observeEvent(this) { navDirections ->

            //navigating to the fragment
            navController.navigateSafe(navDirections)
        }

        //list layout observer
        viewModelHome.listLayoutMode.observe(this) { mode ->
            updateListLayoutItemForMode(mode ?: return@observe)
        }

        //layout list visibility
        viewModelMain.searchBarLayoutListButton.observeEvent(this) { visibility ->
            binding.searchBarView.layoutChangeButton.visibility = visibility
        }

        //call from onResume to handle note create event
        viewModelMain.createNoteEvent.observeEvent(this) { newNoteData ->
            navController.navigateSafe(
                HomeFragmentDirections.homeToAddEditNoteFragment(
                    type = newNoteData.type.value,
                    title = newNoteData.title,
                    content = newNoteData.content
                )
            )
        }

        //close the drawer
        viewModelMain.drawerCloseEvent.observeEvent(this) {
            binding.drawerLayout.closeDrawers()
        }

        //onBack pressed
        viewModelHome.closeAppBackPressedEvent.observeEvent(this) {
            onBackPressedDispatcher.onBackPressed()
        }

        //bottom bar listener event
        viewModelMain.bottomBarItemsListeners.observeEvent(this) { fragment ->

            if (fragment == "draw_note") {
                binding.searchBarView.searchBarView.visibility = View.GONE
                binding.toolbarLayout.toolbarLayout.visibility = View.GONE
            } else {
                binding.searchBarView.searchBarView.visibility = View.GONE
                binding.toolbarLayout.toolbarLayout.visibility = View.VISIBLE

                //normalize toolbar
                normalizeToolbarLabel_NotePage()
            }
        }

        //onFragment event
        viewModelMain.bottomFragmentItemDetachEvent.observeEvent(this) {
            binding.searchBarView.searchBarView.visibility = View.VISIBLE
            binding.toolbarLayout.toolbarLayout.visibility = View.GONE
        }

        //Event of selected items list change
        viewModelHome.onItemSelectEvent.observeEvent(this) { selectionIsEmpty ->

            if (selectionIsEmpty) {

                binding.searchBarView.searchBarView.visibility = View.GONE
                binding.toolbarLayout.toolbarLayout.visibility = View.VISIBLE
                //normalize the view according to the home page view
                when (currentHomeDestination) {
                    HomeDestination.Status(NoteStatus.ARCHIVED) -> normalizeToolbarArchivePage()
                    HomeDestination.Status(NoteStatus.DELETED) -> normalizeToolbarTrashPage()
                    else -> normalizeToolbarHomePage()
                }
            }
            else {
                binding.searchBarView.searchBarView.visibility = View.VISIBLE
                binding.toolbarLayout.toolbarLayout.visibility = View.GONE
            }
        }

        //moved items to the folder now handle the toolbar
        viewModelHome.moveToFolderOperationCompleteEvent.observeEvent(this) {
            normalizeToolbarHomePage()

            //now make the search bar visible and toolbar invisible
            binding.toolbarLayout.toolbarLayout.visibility = View.GONE
            binding.searchBarView.searchBarView.visibility = View.VISIBLE

            //move to the list screen (from where items were selected) or simply pop the current screen and remove the dialog
            val host = supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
            val navController = host.navController
            navController.setGraph(R.navigation.home_nav_graph)

            if(viewModelMain.noteMoveFolder.isNotEmpty()){

                navController.setGraph(R.navigation.main_navigation)
                //fire the event to handle the view
                viewModelMain.bottomBarItemsListenerEvent("list_note")
                //move to the respective fragment
                val note = viewModelHome.getClickedNote()

                when (viewModelMain.noteMoveFolder) {
                    "speech" -> navController.navigateSafe(HomeFragmentDirections.homeToSpeechFragment(note.id))
                    "list" -> navController.navigateSafe(HomeFragmentDirections.homeToTodoFragment(note.id))
                    "text" -> navController.navigateSafe(HomeFragmentDirections.homeToAddEditNoteFragment(note.id))
                }
            }
            else{

                navController.popBackStack()
                navController.setGraph(R.navigation.main_navigation)
                //make selection list empty
                viewModelMain.bottomFragmentItemDetachEventEvent()
                viewModelHome.clearSelection()
                viewModelHome.adapter.selectionFolder = false
            }

            //show the snackbar after moving the item to the desired folder
            val snackbar = Snackbar.make(binding.root, "Moved!", Snackbar.LENGTH_SHORT)
            // Set the background color of the Snack-bar
            snackbar.setBackgroundTint(
                ContextCompat.getColor(
                    this,
                    R.color.search_bar_background
                )
            ) // Replace with your desired color

            // Optionally set the text color
            snackbar.setTextColor(ContextCompat.getColor(this, R.color.dialog_text_color))
            snackbar.show()
        }

        //note unlock dialog event
        viewModelHome.noteLockedEvent.observeEvent(this) { _ ->

            unLockNoteDialog.isCancelable = false
            unLockNoteDialog.showRemoveLockDialog = false
            unLockNoteDialog.show(supportFragmentManager, unLockNoteDialog.tag)
        }
        viewModelHome.statusChangeEvent.observeEvent(this) { statusChange ->
            viewModelHome.onStatusChange(statusChange)
        }

        //move folder from dialog in AddEditFragment
        viewModelHome.moveToFolderToolbarNormalizeEvent.observeEvent(this) {
            //normalize the toolbar here according to the view of selection
            normalizeToolbarForFolderSelection()
        }

        //normalize the toolbar for camera
        viewModelHome.cameraToolbarNormalizeEvent.observeEvent(this) {
            normalizeToolbarCameraPage()
            //making toolbar visible and searchbar invisible
            binding.searchBarView.searchBarView.visibility = View.GONE
            binding.toolbarLayout.toolbarLayout.visibility = View.VISIBLE
        }

        //move items to folder event
        viewModelHome.moveToFolderSelectionEvent.observeEvent(this) { noteType ->

            viewModelMain.noteMoveFolder = noteType
            //check if the current fragment is homeFragment
            //sending another event to folderListFragment so that selection view should be visible to the user
            val host = supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
            val navController = host.navController
            navController.setGraph(R.navigation.home_nav_graph)
            navController.navigateSafe(HomeNavGraphDirections.toFolderListFragment())
            //selection list event
            viewModelHome.folderSelectionListEvent()
        }

        //move items to folder observer
        viewModelHome.moveToFolderActionEvent.observeEvent(this) {
            //write the selected folder id to the all of the selected notes in the selectedNotes list

            //move the notes in the selection list to the folder by putting selected folder id in all of the
            //selected notes and then update the database
            val notesWithFolder = ArrayList<Note>()
            if (viewModelHome.selectedFolder != null){
                for (i in 0..<viewModelHome.selectedNotes.size) {

                    //get the n item and update it
                    val note = viewModelHome.selectedNotes.elementAt(i)
                    note.folderId = viewModelHome.selectedFolder!!.folder.id
                    notesWithFolder.add(note)
                }

                //after updating the selected notes give the selected notes list to the database update function
                viewModelHome.updateSelectedNotes(notesWithFolder)

                //send the event to normalize the toolbar according to the home page
                viewModelHome.moveToFolderOperationCompleted()
            }
            else
                Toast.makeText(this@QuickNotepadMain,"Please select a folder!",Toast.LENGTH_SHORT).show()
        }

        //handling view for label screen
        viewModelMain.labelCreateEvent.observeEvent(this){
            binding.searchBarView.searchBarView.visibility = View.GONE
            binding.toolbarLayout.toolbarLayout.visibility = View.VISIBLE

            //making toolbar for create label event
            normalizeToolbarLabel_NotePage()
        }

        //empty bin dialog event
        viewModelHome.showEmptyTrashDialogEvent.observeEvent(this) {

            //show empty Trash dialog
            showDeleteDialog(true)
        }

        viewModelHome.messageEvent.observeEvent(this) { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                .setGestureInsetBottomIgnored(true)
                .show()
        }

        viewModelAddEdit.messageEvent.observeEvent(this) { message ->
            when (message) {
                EditMessage.BLANK_NOTE_DISCARDED -> viewModelHome.onBlankNoteDiscarded()
                EditMessage.RESTORED_NOTE -> Snackbar.make(binding.root, resources.getQuantityText(
                    R.plurals.edit_message_move_restore, 1), Snackbar.LENGTH_SHORT)
                    .setGestureInsetBottomIgnored(true)
                    .show()

                EditMessage.CANT_EDIT_IN_TRASH -> restoreNoteSnackbar.show()
            }
        }
    }

    private fun normalizeToolbarCameraPage() {

        binding.toolbarLayout.backButton.visibility = View.VISIBLE
        binding.toolbarLayout.moveToFolderButtonAction.visibility = View.GONE
        binding.toolbarLayout.menuIcon.visibility = View.GONE
        binding.toolbarLayout.lockButton.visibility = View.GONE
        binding.toolbarLayout.labelButton.visibility = View.GONE
        binding.toolbarLayout.undoRedo.visibility = View.GONE
        binding.toolbarLayout.archiveButton.visibility = View.GONE
        binding.toolbarLayout.colorButton.visibility = View.GONE
        binding.toolbarLayout.pinButton.visibility = View.GONE
        binding.toolbarLayout.reminderButton.visibility = View.GONE
        binding.toolbarLayout.deleteAction.visibility = View.GONE
        binding.toolbarLayout.restoreAction.visibility = View.GONE
    }

    private fun normalizeToolbarArchivePage() {

        binding.toolbarLayout.backButton.visibility = View.VISIBLE
        binding.toolbarLayout.moveToFolderButtonAction.visibility = View.GONE
        binding.toolbarLayout.menuIcon.visibility = View.GONE
        binding.toolbarLayout.lockButton.visibility = View.GONE
        binding.toolbarLayout.labelButton.visibility = View.GONE
        binding.toolbarLayout.undoRedo.visibility = View.GONE
        binding.toolbarLayout.archiveButton.visibility = View.VISIBLE
        binding.toolbarLayout.colorButton.visibility = View.GONE
        binding.toolbarLayout.pinButton.visibility = View.GONE
        binding.toolbarLayout.reminderButton.visibility = View.GONE
        binding.toolbarLayout.deleteAction.visibility = View.VISIBLE
        binding.toolbarLayout.restoreAction.visibility = View.GONE
    }

    private fun normalizeToolbarTrashPage() {

        binding.toolbarLayout.backButton.visibility = View.VISIBLE
        binding.toolbarLayout.moveToFolderButtonAction.visibility = View.GONE
        binding.toolbarLayout.menuIcon.visibility = View.GONE
        binding.toolbarLayout.lockButton.visibility = View.GONE
        binding.toolbarLayout.labelButton.visibility = View.GONE
        binding.toolbarLayout.undoRedo.visibility = View.GONE
        binding.toolbarLayout.archiveButton.visibility = View.GONE
        binding.toolbarLayout.colorButton.visibility = View.GONE
        binding.toolbarLayout.pinButton.visibility = View.GONE
        binding.toolbarLayout.reminderButton.visibility = View.GONE
        binding.toolbarLayout.deleteAction.visibility = View.VISIBLE
        binding.toolbarLayout.restoreAction.visibility = View.VISIBLE
    }

    private fun normalizeToolbarHomePage() {

        binding.toolbarLayout.backButton.visibility = View.VISIBLE
        binding.toolbarLayout.moveToFolderButtonAction.visibility = View.GONE
        binding.toolbarLayout.menuIcon.visibility = View.VISIBLE
        binding.toolbarLayout.lockButton.visibility = View.VISIBLE
        binding.toolbarLayout.labelButton.visibility = View.VISIBLE
        binding.toolbarLayout.undoRedo.visibility = View.GONE
        binding.toolbarLayout.archiveButton.visibility = View.GONE
        binding.toolbarLayout.colorButton.visibility = View.GONE
        binding.toolbarLayout.deleteAction.visibility = View.GONE
        binding.toolbarLayout.restoreAction.visibility = View.GONE
        binding.toolbarLayout.pinButton.visibility = View.VISIBLE
        binding.toolbarLayout.reminderButton.visibility = View.VISIBLE
    }

    private fun normalizeToolbarLabel_NotePage() {

        binding.toolbarLayout.backButton.visibility = View.VISIBLE
        binding.toolbarLayout.moveToFolderButtonAction.visibility = View.GONE
        binding.toolbarLayout.menuIcon.visibility = View.GONE
        binding.toolbarLayout.lockButton.visibility = View.GONE
        binding.toolbarLayout.labelButton.visibility = View.GONE
        binding.toolbarLayout.undoRedo.visibility = View.GONE
        binding.toolbarLayout.colorButton.visibility = View.GONE
        binding.toolbarLayout.deleteAction.visibility = View.GONE
        binding.toolbarLayout.restoreAction.visibility = View.GONE
        binding.toolbarLayout.pinButton.visibility = View.VISIBLE
        binding.toolbarLayout.reminderButton.visibility = View.VISIBLE
        binding.toolbarLayout.archiveButton.visibility = View.VISIBLE
    }

    //list layout function
    private fun updateListLayoutItemForMode(mode: NoteListLayoutMode) {
        val layoutItem = binding.searchBarView.layoutChangeButton
        when (mode) {
            NoteListLayoutMode.LIST -> {
                layoutItem.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.grid_icon
                    )
                )
            }

            NoteListLayoutMode.GRID -> {
                layoutItem.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.sort_icon
                    )
                )
            }
        }
    }

    //account button functions
    private fun showAccountSignInDialog() {

        //bottom sheet dialog add items
        signin.isCancelable = true
        signin.show(supportFragmentManager, signin.tag)
    }

    private fun showAccountSignedUserInDialog() {
        // Create a dialog instance
        val dialog = Dialog(this)

        // Set the content view to your custom layout
        dialog.setContentView(R.layout.account_dialog)

        dialog.let {

            val windowManager = windowManager
            val display = windowManager.defaultDisplay
            val size = android.graphics.Point()
            display.getSize(size)
            val screenWidth = size.x
            val screenHeight = size.y

            val dialogWidth = (screenWidth * 1.0).toInt()
            val dialogHeight = (screenHeight * 0.6).toInt()

            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = dialogWidth
            layoutParams.height = dialogHeight
            dialog.window?.attributes = layoutParams
            dialog.window?.setGravity(Gravity.CENTER)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.setCancelable(false)
        }

        // Find views in the custom layout
        val closeButton: AppCompatImageButton = dialog.findViewById(R.id.close_button)
        val another_account: AppCompatTextView = dialog.findViewById(R.id.add_another_account)
        val manage_account: AppCompatTextView = dialog.findViewById(R.id.manage_account)
        val userName: AppCompatTextView = dialog.findViewById(R.id.username)
        val email: AppCompatTextView = dialog.findViewById(R.id.email)

        //setting username and email
        if (SharedPreference.email != null && SharedPreference.username != null) {
            email.text = SharedPreference.email
            userName.text = SharedPreference.username
        }

        // Set click listeners
        closeButton.setOnClickListener {
            // Dismiss the dialog when the button is clicked
            dialog.dismiss()
        }

        another_account.setOnClickListener {
            dialog.dismiss()
        }

        manage_account.setOnClickListener {
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }

    private fun handleIntent() {
        val intent = intent ?: return
        if (!intent.getBooleanExtra(KEY_INTENT_HANDLED, false)) {
            when (intent.action) {
                Intent.ACTION_SEND -> {
                    // Plain text was shared to app, create new note for it
                    val noteData = createNoteFromIntent(intent)
                    if (noteData != null) {
                        viewModelMain.createNote(noteData)
                    }
                }

                INTENT_ACTION_CREATE -> {
                    // Intent to create a note of a certain type. Used by launcher shortcuts.
                    val type = NoteTypeConverter.toType(
                        intent.getIntExtra(EXTRA_NOTE_TYPE, 0)
                    )
                    viewModelMain.createNote(MainViewModel.NewNoteData(type))
                }

                INTENT_ACTION_EDIT -> {
                    // Intent to edit a specific note. This is used by reminder notification.
                    viewModelMain.editNote(
                        intent.getLongExtra(
                            AlarmReceiver.EXTRA_NOTE_ID,
                            Note.NO_ID
                        )
                    )
                }

                INTENT_ACTION_SHOW_REMINDERS -> {
                    // Show reminders screen in HomeFragment. Used by launcher shortcut.
                    binding.drawerNav.menu.findItem(R.id.drawer_item_reminder).isChecked = true
                    //setting destination to fetch respective
                    viewModelHome.setDestination(HomeDestination.Reminders)
                }
            }

            // Mark intent as handled or it will be handled again if activity is resumed again.
            intent.putExtra(KEY_INTENT_HANDLED, true)
        }
    }

    private fun createNoteFromIntent(intent: Intent): MainViewModel.NewNoteData? {
        val extras = intent.extras ?: return null
        var noteData: MainViewModel.NewNoteData? = null
        if (intent.type == "text/plain") {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                // A file was shared
                @Suppress("DEPRECATION")
                val uri = extras.get(Intent.EXTRA_STREAM) as? Uri
                if (uri != null) {
                    try {
                        val reader = InputStreamReader(contentResolver.openInputStream(uri))
                        val title = uri.pathSegments.last()
                        val content = reader.readText()
                        noteData = MainViewModel.NewNoteData(NoteType.TEXT, title, content)
                        reader.close()
                    } catch (e: IOException) {
                        // nothing to do (file doesn't exist, access error, etc)
                    }
                }
            } else {
                // Text was shared
                val title = extras.getString(Intent.EXTRA_TITLE)
                    ?: extras.getString(Intent.EXTRA_SUBJECT) ?: ""
                val content = extras.getString(Intent.EXTRA_TEXT) ?: ""
                noteData = MainViewModel.NewNoteData(NoteType.TEXT, title, content)
            }
        }
        return noteData
    }

    override fun close() = signin.dismissNow()

    override fun onResume() {
        super.onResume()

        handleIntent()
    }

    override fun closeUnLocker() = unLockNoteDialog.dismissNow()

    override fun unLockNote(position: Int, lockedNote: NoteItem) {

        //unlocking the note
        viewModelHome.noteUnLocked(lockedNote, position)
        unLockNoteDialog.dismissNow()
    }

    override fun updatePasswordOfNote(newPassword: String, noteItem: NoteItem) {

        val lockedNote = ArrayList<Note>()
        for (noteSelected in viewModelHome.selectedNotes) {

            noteSelected.lock = newPassword
            lockedNote.add(noteSelected)

            //update the selection not
            viewModelHome.updateSelectedNotes(lockedNote)
        }

        //dismissing dialog now
        unLockNoteDialog.dismissNow()
    }

    override fun closeLocker() = createLockNoteDialog.dismissNow()

    override fun addLockToNote(password: String) {

        //update the selected note object in database
        //Notes from selected notes will be get and then
        // will be updated in the note object of the home viewModel
        val lockedNote = ArrayList<Note>()
        for (noteSelected in viewModelHome.selectedNotes) {

            noteSelected.lock = password
            lockedNote.add(noteSelected)

            //update the selection not
            viewModelHome.updateSelectedNotes(lockedNote)
            //then save the password to preference
            SharedPreference.noteLock = true
        }

        //dismissing dialog now
        createLockNoteDialog.dismissNow()
        //clearing selection
        viewModelHome.clearSelection()
        //normalizing view
        viewModelMain.bottomFragmentItemDetachEventEvent()
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        binding.drawerLayout.setDrawerLockMode(
            if (destination.id == R.id.homeFragment)
                DrawerLayout.LOCK_MODE_UNLOCKED
            else
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with audio recording
                //creating note for it
                viewModelHome.createNote()
                //fire the event to handle the view
                viewModelMain.bottomBarItemsListenerEvent("speech_note")
            } else {
                // Permission denied, inform the user or handle it gracefully
                // You may display a message or disable audio recording feature
                ActivityCompat.requestPermissions(
                    this@QuickNotepadMain,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
        else if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with audio recording

                val currentFragId = navController.currentBackStackEntry!!.destination.id

                if(currentFragId == R.id.addEditFragment ||
                    currentFragId == R.id.speechFragment ||
                    currentFragId == R.id.todoFragment &&
                    !viewModelHome.openGalleryEventVal){

                    //take photo navigate to camera screen
                    val host = supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                    val navController = host.navController
                    navController.setGraph(R.navigation.main_navigation)

                    //send the event to normalize the toolbar
                    viewModelHome.cameraToolbarNormalizeEvent()
                    //navigation
                    navController.navigateSafe(MainNavigationDirections.mainToCameraFragment(false))
                }
                else{
                    //opening the gallery
                    viewModelMain.openGalleryEvent()
                }
            }
            else {
                //take runtime image collection permission and add the image to the parent layout background
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                    if (ContextCompat.checkSelfPermission(this@QuickNotepadMain, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {

                        val currentFragId = navController.currentBackStackEntry!!.destination.id

                        if(currentFragId == R.id.addEditFragment ||
                            currentFragId == R.id.speechFragment ||
                            currentFragId == R.id.todoFragment &&
                            !viewModelHome.openGalleryEventVal){

                            //take photo navigate to camera screen
                            val host = supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                            val navController = host.navController
                            navController.setGraph(R.navigation.main_navigation)

                            //send the event to normalize the toolbar
                            viewModelHome.cameraToolbarNormalizeEvent()
                            //navigation
                            navController.navigateSafe(MainNavigationDirections.mainToCameraFragment(false))
                        }
                        else{
                            //opening the gallery
                            viewModelMain.openGalleryEvent()
                        }
                    }
                    else {
                        // Request the permissions
                        ActivityCompat.requestPermissions(
                            this@QuickNotepadMain,
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                            REQUEST_CODE_STORAGE
                        )
                    }
                } else {

                    if (MainUtils.hasStoragePermission(this@QuickNotepadMain)) {
                        //opening the gallery
                        viewModelMain.openGalleryEvent()
                    } else {
                        //requesting the Gallery permission
                        MainUtils.getStoragePermission(this@QuickNotepadMain)
                    }
                }
            }
        }
        else if (requestCode == REQ_CODE_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                //Permission granted. Now, check that whether the current fragment is AddEditFragment or not
                val currentFragId = navController.currentBackStackEntry!!.destination.id

                if(currentFragId == R.id.addEditFragment ||
                    currentFragId == R.id.speechFragment ||
                    currentFragId == R.id.todoFragment){

                    //now this means the fragment is called from with in the AddEditFragment
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                        if (ContextCompat.checkSelfPermission(this@QuickNotepadMain, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {

                            //take photo navigate to camera screen
                            val host = supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                            val navController = host.navController
                            navController.setGraph(R.navigation.main_navigation)

                            //send the event to normalize the toolbar
                            viewModelHome.cameraToolbarNormalizeEvent()
                            //navigation
                            navController.navigateSafe(MainNavigationDirections.mainToCameraFragment(false))
                        }
                        else {
                            // Request the permissions
                            ActivityCompat.requestPermissions(this@QuickNotepadMain, arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                                AddEditNoteBottomSheetDialog.REQUEST_CODE_STORAGE
                            )
                        }
                    }
                    else {

                        if (MainUtils.hasStoragePermission(this@QuickNotepadMain)) {
                            //take photo navigate to camera screen
                            val host = supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment_container) as NavHostFragment
                            val navController = host.navController
                            navController.setGraph(R.navigation.main_navigation)

                            //send the event to normalize the toolbar
                            viewModelHome.cameraToolbarNormalizeEvent()
                            //navigation
                            navController.navigateSafe(MainNavigationDirections.mainToCameraFragment(false))
                        }
                        else {
                            //requesting the Gallery permission
                            MainUtils.getStoragePermission(this@QuickNotepadMain)
                        }
                    }
                }
                else{

                    //send the event to normalize the toolbar
                    viewModelHome.cameraToolbarNormalizeEvent()
                    //navigation
                    navController.navigateSafe(
                        HomeFragmentDirections.actionHomeFragmentToCameraFragment(
                            viewModelHome.scanScreen
                        )
                    )
                }
            }
            else {
                //permission denied
                //ask for permission
                ActivityCompat.requestPermissions(
                    this@QuickNotepadMain,
                    arrayOf(Manifest.permission.CAMERA),
                    REQ_CODE_CAMERA
                )
            }
        }
    }

    companion object {
        private const val KEY_INTENT_HANDLED = "com.maltaisn.notes.INTENT_HANDLED"
        const val EXTRA_NOTE_TYPE = "com.maltaisn.notes.NOTE_TYPE"
        const val INTENT_ACTION_CREATE = "com.maltaisn.notes.CREATE"
        const val INTENT_ACTION_EDIT = "com.maltaisn.notes.EDIT"
        const val INTENT_ACTION_SHOW_REMINDERS = "com.maltaisn.notes.SHOW_REMINDERS"
        private const val CANT_EDIT_SNACKBAR_DURATION = 5000
    }
}