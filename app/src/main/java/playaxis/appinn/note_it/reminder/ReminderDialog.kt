package playaxis.appinn.note_it.reminder

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.format.RecurrenceFormatter
import com.maltaisn.recurpicker.list.RecurrenceListCallback
import com.maltaisn.recurpicker.list.RecurrenceListDialog
import com.maltaisn.recurpicker.picker.RecurrencePickerCallback
import com.maltaisn.recurpicker.picker.RecurrencePickerDialog
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.common.ConfirmDialog
import playaxis.appinn.note_it.databinding.DialogReminderBinding
import playaxis.appinn.note_it.extensions.contains
import playaxis.appinn.note_it.extensions.setMaxWidth
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.utils.observeEvent
import playaxis.appinn.note_it.main.viewModels.navGraphViewModel
import playaxis.appinn.note_it.notification.NotificationPermission
import java.text.DateFormat
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

class ReminderDialog : DialogFragment(), RecurrenceListCallback, RecurrencePickerCallback, ConfirmDialog.Callback {

    @Inject
    lateinit var viewModelFactory: ReminderViewModel.Factory
    private val viewModel by navGraphViewModel(R.id.nav_graph_reminder) { viewModelFactory.create(it) }

    private val args: ReminderDialogArgs by navArgs()

    private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
    private val recurrenceFormat = RecurrenceFormatter(dateFormat)

    private var notificationPermission: NotificationPermission? = null
    private var reminderPermission: ReminderPermission? = null
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (requireContext().applicationContext as QuickNotepad).appComponent.inject(this)
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val binding = DialogReminderBinding.inflate(layoutInflater, null, false)

        binding.dateForegroundView.setOnClickListener {
            viewModel.onDateClicked()
        }
        binding.timeForegroundView.setOnClickListener {
            viewModel.onTimeClicked()
        }
        binding.recurrenceForegroundView.setOnClickListener {
            viewModel.onRecurrenceClicked()
        }

        // Create dialog
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setBackground(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.reminder_background
                )
            )
            .setPositiveButton(R.string.action_ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        //setting title
        val customTitleView = layoutInflater.inflate(R.layout.custom_dialog_title, null)
        val titleTextView = customTitleView.findViewById<AppCompatTextView>(R.id.title_txv)
        titleTextView.text = getString(R.string.action_reminder_add)
        titleTextView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.dialog_text_color
            )
        )
        dialog.setCustomTitle(customTitleView)

        dialog.setOnShowListener {
            dialog.setMaxWidth(
                context.resources.getDimensionPixelSize(R.dimen.reminder_dialog_max_width),
                binding.root
            )

            //Setting text color for the buttons
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.dialog_text_color))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.dialog_text_color))
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.dialog_text_color))
            onDialogShown(dialog)
        }

        setupViewModelObservers(binding)

        if (savedInstanceState != null) {
            // Update dialog listeners if needed to avoid them referencing the old fragment instance,
            // creating a memory leak and preventing correct callback sequence.
            val timePicker =
                childFragmentManager.findFragmentByTag(TIME_DIALOG_TAG) as MaterialTimePicker?
            if (timePicker != null) {
                timePicker.clearOnPositiveButtonClickListeners()
                registerTimePickerListener(timePicker)
            }

            @Suppress("UNCHECKED_CAST")
            val datePicker =
                childFragmentManager.findFragmentByTag(DATE_DIALOG_TAG) as MaterialDatePicker<Long>?
            if (datePicker != null) {
                datePicker.clearOnPositiveButtonClickListeners()
                registerDatePickerListener(datePicker)
            }
        }

        if (savedInstanceState == null) {
            notificationPermission = NotificationPermission(this).apply {
                deniedListener = { dismiss() }
                request()
            }
            reminderPermission = ReminderPermission(this, requireContext()).apply {
                deniedListener = { dismiss() }
                request()
            }
        }

        viewModel.start(args.noteIds.toList())

        return dialog
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationPermission = null
        reminderPermission = null
    }

    private fun setupViewModelObservers(binding: DialogReminderBinding) {
        // Using `this` as lifecycle owner, cannot show dialog twice with same instance to avoid double observation.

        viewModel.details.observe(this) { details ->
            binding.dateInput.setText(dateFormat.format(details.date))
            binding.timeInput.setText(timeFormat.format(details.date))
            binding.recurrenceTxv.text = recurrenceFormat.format(
                requireContext(),
                details.recurrence, details.date
            )
        }

        viewModel.invalidTime.observe(this) { invalid ->
            binding.invalidTimeTxv.isVisible = invalid
        }

        viewModel.showDateDialogEvent.observeEvent(this) { date ->

            //Date Dialog
            val calendarConstraints = CalendarConstraints.Builder()
                .setStart(System.currentTimeMillis())
                .setValidator(DateValidatorPointForward.now())
                .build()

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setCalendarConstraints(calendarConstraints)
                .setSelection(date + TimeZone.getDefault().getOffset(date))
                .setTheme(R.style.MaterialCalendarTheme)
                .build()

            registerDatePickerListener(datePicker)
            datePicker.show(childFragmentManager, DATE_DIALOG_TAG)
        }

        viewModel.showTimeDialogEvent.observeEvent(this) { time ->

            //Time Dialog
            val isUsing24HourFormat = android.text.format.DateFormat.is24HourFormat(context)
            val timeFormat = if (isUsing24HourFormat) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = time

            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(timeFormat)
                .setHour(calendar[Calendar.HOUR_OF_DAY])
                .setMinute(calendar[Calendar.MINUTE])
                .setTheme(R.style.CustomMaterialTimePicker)
                .build()

            registerTimePickerListener(timePicker)
            timePicker.show(childFragmentManager, TIME_DIALOG_TAG)
        }

        viewModel.showRecurrenceListDialogEvent.observeEvent(this) { details ->
            if (RECURRENCE_LIST_DIALOG_TAG !in childFragmentManager) {
                RecurrenceListDialog.newInstance(RecurrencePickerSettings()).apply {
                    startDate = details.date
                    selectedRecurrence = details.recurrence
                    setStyle(STYLE_NORMAL,R.style.AppTheme)
                }.show(childFragmentManager, RECURRENCE_LIST_DIALOG_TAG)
            }
        }

        viewModel.showRecurrencePickerDialogEvent.observeEvent(this) { details ->
            if (RECURRENCE_PICKER_DIALOG_TAG !in childFragmentManager) {
                RecurrencePickerDialog.newInstance(RecurrencePickerSettings()).apply {
                    startDate = details.date
                    selectedRecurrence = details.recurrence
                    setStyle(STYLE_NORMAL, R.style.AppTheme)
                }.show(childFragmentManager, RECURRENCE_PICKER_DIALOG_TAG)
            }
        }

        viewModel.reminderChangeEvent.observeEvent(this) { reminder ->
            homeViewModel.onReminderChange(reminder)
        }

        viewModel.dismissEvent.observeEvent(this) {
            dismiss()
        }
    }

    private fun registerDatePickerListener(picker: MaterialDatePicker<Long>) {

        picker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            // MaterialDatePicker operates on UTC timezone... convert to local timezone (in UTC millis).
            calendar.timeInMillis = selection - TimeZone.getDefault().getOffset(selection)
            viewModel.changeDate(
                calendar[Calendar.YEAR],
                calendar[Calendar.MONTH],
                calendar[Calendar.DAY_OF_MONTH]
            )
        }
    }

    private fun registerTimePickerListener(picker: MaterialTimePicker) {
        picker.addOnPositiveButtonClickListener {
            viewModel.changeTime(picker.hour, picker.minute)
        }
    }

    private fun onDialogShown(dialog: AlertDialog) {
        val okBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        okBtn.setOnClickListener {
            viewModel.createReminder()
        }
        viewModel.invalidTime.observe(this) { invalid ->
            okBtn.isEnabled = !invalid
        }
        val deleteBtn = dialog.getButton(DialogInterface.BUTTON_NEUTRAL)
        deleteBtn.setText(R.string.action_delete)
        deleteBtn.setOnClickListener {
            viewModel.deleteReminder()
        }
        viewModel.isEditingReminder.observe(this) { editing ->
            dialog.setTitle(if (editing) R.string.action_reminder_edit else R.string.action_reminder_add)
        }
        viewModel.isDeleteBtnVisible.observe(this) { visible ->
            deleteBtn.isVisible = visible
        }
    }

    override fun onRecurrenceCustomClicked() {
        viewModel.onRecurrenceCustomClicked()
    }

    override fun onRecurrencePresetSelected(recurrence: Recurrence) {
        viewModel.changeRecurrence(recurrence)
    }

    override fun onRecurrenceCreated(recurrence: Recurrence) {
        viewModel.changeRecurrence(recurrence)
    }

    override fun onDialogPositiveButtonClicked(tag: String?) {
        notificationPermission?.onDialogPositiveButtonClicked(tag)
        reminderPermission?.onDialogPositiveButtonClicked(tag)
    }

    override fun onDialogNegativeButtonClicked(tag: String?) {
        notificationPermission?.onDialogNegativeButtonClicked(tag)
        reminderPermission?.onDialogNegativeButtonClicked(tag)
    }

    override fun onDialogCancelled(tag: String?) {
        notificationPermission?.onDialogCancelled(tag)
        reminderPermission?.onDialogCancelled(tag)
    }

    companion object {
        private const val DATE_DIALOG_TAG = "date-picker-dialog"
        private const val TIME_DIALOG_TAG = "time-picker-dialog"

        private const val RECURRENCE_LIST_DIALOG_TAG = "recurrence-list-dialog"
        private const val RECURRENCE_PICKER_DIALOG_TAG = "recurrence-picker-dialog"
    }
}
