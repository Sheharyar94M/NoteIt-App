package playaxis.appinn.note_it.repository

import android.annotation.SuppressLint
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.repository.appData.AppDataRepository
import playaxis.appinn.note_it.repository.model.entities.ColorNote
import playaxis.appinn.note_it.repository.model.entities.GradientNote
import playaxis.appinn.note_it.repository.model.entities.helper.ImageNoteBackground
import javax.inject.Inject

class DefaultAppDataRepository @Inject constructor(): AppDataRepository {
    @SuppressLint("Recycle")
    override fun getColors(): MutableLiveData<ArrayList<ColorNote>> {

        val colors = ArrayList<ColorNote>()

        colors.add(ColorNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.color_1).toString()))
        colors.add(ColorNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.color_2).toString()))
        colors.add(ColorNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.color_3).toString()))
        colors.add(ColorNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.color_4).toString()))
        colors.add(ColorNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.color_5).toString()))
        colors.add(ColorNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.color_6).toString()))
        colors.add(ColorNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.color_7).toString()))
        colors.add(ColorNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.color_8).toString()))
        colors.add(ColorNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.color_9).toString()))
        colors.add(ColorNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.color_10).toString()))
        colors.add(ColorNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.color_11).toString()))


        val colorsLiveData = MutableLiveData<ArrayList<ColorNote>>()
        colorsLiveData.value = colors
        return colorsLiveData
    }
    override fun getGradientColors(): MutableLiveData<ArrayList<GradientNote>> {

        val gradient = ArrayList<GradientNote>()

        gradient.add(GradientNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.item_1_0),ContextCompat.getColor(QuickNotepad.appContext, R.color.item_1_1)))
        gradient.add(GradientNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.item_2_0),ContextCompat.getColor(QuickNotepad.appContext, R.color.item_2_1)))
        gradient.add(GradientNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.item_3_0),ContextCompat.getColor(QuickNotepad.appContext, R.color.item_3_1)))
        gradient.add(GradientNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.item_4_0),ContextCompat.getColor(QuickNotepad.appContext, R.color.item_4_1)))
        gradient.add(GradientNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.item_5_0),ContextCompat.getColor(QuickNotepad.appContext, R.color.item_5_1)))
        gradient.add(GradientNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.item_6_0),ContextCompat.getColor(QuickNotepad.appContext, R.color.item_6_1)))
        gradient.add(GradientNote(ContextCompat.getColor(QuickNotepad.appContext, R.color.item_7_0),ContextCompat.getColor(QuickNotepad.appContext, R.color.item_7_1)))

        Log.i("getGradientColors1: ",gradient.toString())

        val gradientLiveData = MutableLiveData<ArrayList<GradientNote>>()
        gradientLiveData.value = gradient
        return gradientLiveData
    }
    override fun getWallpaperImages(): MutableLiveData<ArrayList<ImageNoteBackground>> {

        val wallpapers = ArrayList<ImageNoteBackground>()

        wallpapers.add(ImageNoteBackground(ContextCompat.getDrawable(QuickNotepad.appContext, R.drawable.wallpaper_1)!!))
        wallpapers.add(ImageNoteBackground(ContextCompat.getDrawable(QuickNotepad.appContext, R.drawable.wallpaper_2)!!))
        wallpapers.add(ImageNoteBackground(ContextCompat.getDrawable(QuickNotepad.appContext, R.drawable.wallpaper_3)!!))
        wallpapers.add(ImageNoteBackground(ContextCompat.getDrawable(QuickNotepad.appContext, R.drawable.wallpaper_4)!!))
        wallpapers.add(ImageNoteBackground(ContextCompat.getDrawable(QuickNotepad.appContext, R.drawable.wallpaper_5)!!))
        wallpapers.add(ImageNoteBackground(ContextCompat.getDrawable(QuickNotepad.appContext, R.drawable.wallpaper_6)!!))
        wallpapers.add(ImageNoteBackground(ContextCompat.getDrawable(QuickNotepad.appContext, R.drawable.wallpaper_7)!!))
        wallpapers.add(ImageNoteBackground(ContextCompat.getDrawable(QuickNotepad.appContext, R.drawable.wallpaper_7)!!))

        val wallpapersLiveData = MutableLiveData<ArrayList<ImageNoteBackground>>()
        wallpapersLiveData.value = wallpapers
        return wallpapersLiveData
    }
}