package playaxis.appinn.note_it.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.canvas.DrawingView
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.helper.SerializableDrawingPath
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.vectorfinder.VectorDrawableCompat
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.helper_model.CustomTypefaceSpan
import playaxis.appinn.note_it.repository.model.entities.Note
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class MainUtils {

    companion object {

        private var REQUEST_CODE_STORAGE = 3

        fun makeStatusBarTransparent(activity: Activity) {
            activity.window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            val window = activity.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        }

        fun statusBarColor(activity: Activity,color: Int) {
            activity.window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            val window = activity.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = color
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun hasPermissions(context: Context): Boolean {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.POST_NOTIFICATIONS)

            return context.checkCallingOrSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED &&
                    context.checkCallingOrSelfPermission(permissions[1]) == PackageManager.PERMISSION_GRANTED}

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun getPermissions(activity: Activity) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.POST_NOTIFICATIONS)
            activity.requestPermissions(permissions, REQUEST_CODE_STORAGE)
        }

        fun permissionSettingsPage(activity: Activity) {
            activity.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package: " + activity.packageName)
                )
            )
        }

        fun hasStoragePermission(context: Context): Boolean {
            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return context.checkCallingOrSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED &&
                    context.checkCallingOrSelfPermission(permissions[1]) == PackageManager.PERMISSION_GRANTED
        }
        fun getStoragePermission(activity: Activity) {
            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
            activity.requestPermissions(permissions, REQUEST_CODE_STORAGE)
        }

        //replace fragment
        fun replaceFragment(fragment: Fragment, fragmentManager: FragmentManager, fragmentContainer: Int, tag: String) {
            // Begin a transaction to add the fragment to the layout
            val transaction: FragmentTransaction = fragmentManager.beginTransaction()
            transaction.replace(fragmentContainer, fragment,tag)
            transaction.commit()
        }

        //add fragment
        fun addFragment(fragment: Fragment, fragmentManager: FragmentManager, fragmentContainer: Int, tag: String) {
            // Begin a transaction to add the fragment to the layout
            val transaction: FragmentTransaction = fragmentManager.beginTransaction()
            transaction.add(fragmentContainer, fragment,tag)
            transaction.addToBackStack(tag)
            transaction.commit()
        }

        fun finishFragment(fragmentManager: FragmentManager, fragment: Fragment) {

            // Remove the fragment from the activity
            val transaction = fragmentManager.beginTransaction()
            transaction.remove(fragment).commit()
        }

        fun drawableToString(drawable: Drawable?): String {

            drawable.let {

                val bitmap = drawableToBitmap(drawable)
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                return Base64.encodeToString(byteArray, Base64.DEFAULT)
            }
        }

        fun stringToDrawable(encodedString: String): Drawable {
            val decodedByteArray = Base64.decode(encodedString, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)
            return BitmapDrawable(null, bitmap)
        }

        private fun drawableToBitmap(drawable: Drawable?): Bitmap {
            drawable.let {

                if (drawable is BitmapDrawable) {
                    return drawable.bitmap
                }
                val bitmap = Bitmap.createBitmap(
                    drawable!!.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                return bitmap
            }
        }

        fun gradientToBitmap(drawable: Drawable?): Bitmap {
            // Ensure drawable is not null
            drawable ?: throw IllegalArgumentException("Drawable must not be null")

            // If the drawable is already a BitmapDrawable, just return its bitmap
            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            }

            // If the drawable has no intrinsic width and height, we assign default values
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1

            // Create a Bitmap object to the drawable's dimensions
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Set the drawable's bounds and draw it onto the canvas
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        }

        fun stringToBitmap(imageString: String): Bitmap? {
            val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
            if (imageBytes.isEmpty()) {
                return null
            }
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
        fun bitmapToString(bitmap: Bitmap): String {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val b = baos.toByteArray()
            return Base64.encodeToString(b, Base64.DEFAULT)
        }

        fun uriToBitmap(context: Context, uri: Uri?): Bitmap? {
            try {
                val inputStream = context.contentResolver.openInputStream(uri!!)
                if (inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    return bitmap
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        fun listToString(list: ArrayList<String>, delimiter: String = ","): String {
            return list.joinToString(delimiter)
        }

        fun stringToList(input: String, delimiter: String = ","): List<String> {
            return input.split(delimiter)
        }

        fun applyBulletsToString(string: String): SpannableStringBuilder{

            // Create a SpannableStringBuilder
            val spannableStringBuilder = SpannableStringBuilder(string)

            // Find start index of each line
            var start = 0
            while (start < spannableStringBuilder.length) {
                val end = spannableStringBuilder.indexOf('\n', start)
                if (end == -1) break
                spannableStringBuilder.setSpan(
                    BulletSpan(16),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                start = end + 1
            }

            // Apply BulletSpan to the last line
            if (start < spannableStringBuilder.length) {
                spannableStringBuilder.setSpan(
                    BulletSpan(16),
                    start,
                    spannableStringBuilder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            return spannableStringBuilder
        }

        fun removeBulletsFromString(string: String): SpannableStringBuilder {
            // Create a SpannableStringBuilder
            val spannableStringBuilder = SpannableStringBuilder(string)

            // Get all BulletSpan instances
            val bulletSpans = spannableStringBuilder.getSpans(0, spannableStringBuilder.length, BulletSpan::class.java)

            // Remove each BulletSpan
            for (bulletSpan in bulletSpans) {
                spannableStringBuilder.removeSpan(bulletSpan)
            }

            return spannableStringBuilder
        }

        fun serializeSpannableString(spannable: SpannableStringBuilder): String {
            val json = JSONObject()
            json.put("text", spannable.toString())

            val spans = JSONArray()
            spannable.getSpans(0, spannable.length, Any::class.java).forEach { span ->
                val spanJson = JSONObject()
                spanJson.put("start", spannable.getSpanStart(span))
                spanJson.put("end", spannable.getSpanEnd(span))
                spanJson.put("type", span.javaClass.simpleName)

                when (span) {
                    is StyleSpan -> spanJson.put("style", span.style)
                    is UnderlineSpan -> { /* No additional properties */ }
                    is BulletSpan -> spanJson.put("gapWidth", span.getLeadingMargin(true))
                    is AbsoluteSizeSpan -> spanJson.put("size", span.size)
                    is CustomTypefaceSpan -> spanJson.put("fontPath", span.getFontPath())
                    is ForegroundColorSpan -> spanJson.put("color", span.foregroundColor)
                }

                spans.put(spanJson)
            }

            json.put("spans", spans)
            return json.toString()
        }

        @SuppressLint("DiscouragedApi")
        fun deserializeSpannableString(context: Context, serializedNote: Note): SpannableStringBuilder {
            val json = JSONObject(serializedNote.content)
            val spannableStringBuilder = SpannableStringBuilder(json.getString("text"))

            val spans = json.getJSONArray("spans")
            for (i in 0 until spans.length()) {
                val spanJson = spans.getJSONObject(i)
                val start = spanJson.getInt("start")
                val end = spanJson.getInt("end")
                val type = spanJson.getString("type")

                val span = when (type) {
                    "StyleSpan" -> StyleSpan(spanJson.getInt("style"))
                    "UnderlineSpan" -> UnderlineSpan()
                    "BulletSpan" -> BulletSpan(spanJson.getInt("gapWidth"))
                    "AbsoluteSizeSpan" -> AbsoluteSizeSpan(spanJson.getInt("size"))
                    "CustomTypefaceSpan" -> {
                        // get and update list from preference
                        val fontPath = spanJson.getString("fontPath")
                        Log.i("fontPath: ",fontPath)
                        CustomTypefaceSpan(fontPath, Typeface.createFromAsset(context.assets, fontPath))
                    }
                    "ForegroundColorSpan" -> ForegroundColorSpan(spanJson.getInt("color"))
                    else -> Any()
                }

                spannableStringBuilder.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            return spannableStringBuilder
        }

//        //Ad loaders
//        fun getAdaptiveAdSize(context: Context, resources: Resources): AdSize {
//            val widthPixels = resources.displayMetrics.widthPixels
//            val density = resources.displayMetrics.density
//            val widthDp = (widthPixels / density).toInt()
//            val adWidth = widthDp.coerceAtMost(640)
//            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
//        }

//        fun fetchIdsOfAds() {
//
//            val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
//
//            val configSettings = FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(4000).build()
//            firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
//
//            //remote config
//            firebaseRemoteConfig.fetchAndActivate()
//                .addOnCompleteListener { task ->
//
//                    if (task.isSuccessful) {
//
//                        //get the data ids
//                        val adIdsJson = firebaseRemoteConfig.getString("Ad_ids_AdMob")
//                        //get the data banner native networks
//                        val banner_native_networkJson = firebaseRemoteConfig.getString("Banner_Native_Ads_AdMob_AppLovin")
//                        //get the data interstitial openAd networks
//                        val interstitial_open_networkJson = firebaseRemoteConfig.getString("Interstitial_openAd_AppLovin_AdMob")
//
//                        //Ad ids
//                        val parsedValuesConfigIds = JSONObject(adIdsJson)
//
//                        val debugJson = parsedValuesConfigIds.getString("Debug")
//                        val releaseJson = parsedValuesConfigIds.getString("Release")
//
//                        val debug = JSONObject(debugJson)
//                        val release = JSONObject(releaseJson)
//
//                        //Ad Ids Admob {Debug}
//                        LogoMakerApp.BANNER_AD_ADMOB_ID_DEBUG = debug.getString("banner_Ad_Admob_id")
//                        LogoMakerApp.NATIVE_AD_ADMOB_ID_DEBUG = debug.getString("native_Ad_Admob_id")
//                        LogoMakerApp.INTERSTITIAL_AD_ADMOB_ID_DEBUG = debug.getString("interstitial_Ad_Admob_id")
//                        openAd_id_debug_liveData.value = release.getString("openAd_Ad_Admob_id")
//
//                        //Ad Ids Admob {Release}
//                        LogoMakerApp.BANNER_AD_ADMOB_ID_RELEASE = release.getString("banner_Ad_Admob_id")
//                        LogoMakerApp.NATIVE_AD_ADMOB_ID_RELEASE = release.getString("native_Ad_Admob_id")
//                        LogoMakerApp.INTERSTITIAL_AD_ADMOB_ID_RELEASE = release.getString("interstitial_Ad_Admob_id")
//                        openAd_id_release_liveData.value = release.getString("openAd_Ad_Admob_id")
//
//                        //banner and native networks.....
//                        val parsedValuesBannerNativeNetworks = JSONObject(banner_native_networkJson)
//
//                        //splash Screen
//                        splash_banner_liveData.value = "1"
//                        //mainScreen
//                        LogoMakerApp.MAIN_SCREEN_BANNER_BOTTOM = parsedValuesBannerNativeNetworks.getString("MainScreen_banner_bottom")
//                        LogoMakerApp.MAIN_SCREEN_BANNER_TOP = parsedValuesBannerNativeNetworks.getString("MainScreen_banner_top")
//                        LogoMakerApp.MAIN_SCREEN_NATIVE_AD = parsedValuesBannerNativeNetworks.getString("MainScreen_nativeAd")
//                        //recents screen
//                        LogoMakerApp.RECENTS_ACTIVITY_BANNER_TOP = parsedValuesBannerNativeNetworks.getString("RecentsActivity_banner_top")
//                        LogoMakerApp.RECENTS_ACTIVITY_BANNER_BOTTOM = parsedValuesBannerNativeNetworks.getString("RecentsActivity_banner_bottom")
//                        //Preview
//                        LogoMakerApp.PREVIEW_ACTIVITY_BANNER_TOP = parsedValuesBannerNativeNetworks.getString("PreviewActivity_banner_Top")
//                        LogoMakerApp.PREVIEW_ACTIVITY_BANNER_BOTTOM = parsedValuesBannerNativeNetworks.getString("PreviewActivity_banner_bottom")
//                        //creating logo screen
//                        LogoMakerApp.CREATE_LOGO_SCREEN_BANNER_TOP = parsedValuesBannerNativeNetworks.getString("CreateLogoScreen_banner_top")
//                        LogoMakerApp.CREATE_LOGO_SCREEN_BANNER_BOTTOM = parsedValuesBannerNativeNetworks.getString("CreateLogoScreen_banner_bottom")
//                        //exit screen
//                        LogoMakerApp.EXIT_SCREEN_BANNER_TOP = parsedValuesBannerNativeNetworks.getString("ExitScreen_banner_top")
//                        LogoMakerApp.EXIT_SCREEN_BANNER_BOTTOM = parsedValuesBannerNativeNetworks.getString("ExitScreen_banner_bottom")
//
//                        LogoMakerApp.POLICY_ACTIVITY_BANNER_TOP = parsedValuesBannerNativeNetworks.getString("PolicyActivity_banner_top")
//                        LogoMakerApp.POLICY_ACTIVITY_BANNER_BOTTOM = parsedValuesBannerNativeNetworks.getString("PolicyActivity_banner_bottom")
//                        LogoMakerApp.BOARDING_ACTIVITY_BANNER_TOP = parsedValuesBannerNativeNetworks.getString("BoardingActivity_banner_top")
//                        LogoMakerApp.BOARDING_ACTIVITY_BANNER_BOTTOM = parsedValuesBannerNativeNetworks.getString("BoardingActivity_banner_bottom")
//                        LogoMakerApp.PERMISSION_ACTIVITY_BANNER_TOP = parsedValuesBannerNativeNetworks.getString("PermissionActivity_banner_top")
//                        LogoMakerApp.PERMISSION_ACTIVITY_BANNER_BOTTOM = parsedValuesBannerNativeNetworks.getString("PermissionActivity_banner_bottom")
//
//
//                        //interstitial and openAd network.....
//                        val parsedValuesInterstitialOpenAdNetworks = JSONObject(interstitial_open_networkJson)
//
//                        //openAd
//                        LogoMakerApp.SPLASH_OPENAD_AGREE_BUTTON =
//                            parsedValuesInterstitialOpenAdNetworks.getString("Splash_openAd_agree_button")
//                        //mainScreen
//                        LogoMakerApp.MAINSCREEN_CREATELOGO_BUTTON_INTERSTITIAL =
//                            parsedValuesInterstitialOpenAdNetworks.getString("MainScreen_createLogo_Button_interstitial")
//                        LogoMakerApp.MAINSCREEN_EDITLOGO_BUTTON_INTERSTITIAL =
//                            parsedValuesInterstitialOpenAdNetworks.getString("MainScreen_editLogo_Button_interstitial")
//                        LogoMakerApp.MAINSCREEN_RECENTLIST_ITEM_CLICK_INTERSTITIAL =
//                            parsedValuesInterstitialOpenAdNetworks.getString("MainScreen_recentList_item_click_interstitial")
//                        //recents screen
//                        LogoMakerApp.RECENTS_ACTIVITY_RECENTLIST_ITEM_CLICK_INTERSTITIAL =
//                            parsedValuesInterstitialOpenAdNetworks.getString("RecentsActivity_recentList_item_click_interstitial")
//                        //Preview
//                        LogoMakerApp.PREVIEW_ACTIVITY_BACK_BUTTON_PRESS_CLICK_BUTTON_INTERSTITIAL =
//                            parsedValuesInterstitialOpenAdNetworks.getString("PreviewActivity_back_button_press_click_interstitial")
//                        //creating logo screen
//                        LogoMakerApp.CREATE_LOGO_SCREEN_BACK_BUTTON_PRESS_INTERSTITIAL =
//                            parsedValuesInterstitialOpenAdNetworks.getString("CreateLogoScreen_back_button_press_interstitial")
//                        LogoMakerApp.CREATE_LOGO_SCREEN_DOWNLOAD_BUTTON_PRESS_INTERSTITIAL =
//                            parsedValuesInterstitialOpenAdNetworks.getString("CreateLogoScreen_download_button_press_interstitial")
//                        //policy activity
//                        LogoMakerApp.POLICY_ACTIVITY_BUTTON_CLICK_INTERSTITIAL =
//                            parsedValuesInterstitialOpenAdNetworks.getString("PolicyActivity_button_click_interstitial")
//                        //permissions activity
//                        LogoMakerApp.PERMISSION_ACTIVITY_BUTTON_CLICK_INTERSTITIAL =
//                            parsedValuesInterstitialOpenAdNetworks.getString("PermissionActivity_button_click_interstitial")
//                        //boarding activity
//                        LogoMakerApp.BOARDING_ACTIVITY_BUTTON_CLICK_INTERSTITIAL =
//                            parsedValuesInterstitialOpenAdNetworks.getString("BoardingActivity_button_click_interstitial")
//
//                        Log.i("DEBUG_AD_IDs ", LogoMakerApp.BANNER_AD_ADMOB_ID_DEBUG)
//                    } else {
//
//                        Log.i("Task unSuccessful", "No data available")
//                    }
//                }
//                .addOnFailureListener { exception ->
//
//                    Log.i("remoteConfigFailure: ", exception.localizedMessage!!.toString())
//                }
//        }

        fun getVectorDrawable(resources: Resources, resourceId: Int): VectorDrawable {
            // Use VectorDrawableCompat for compatibility
            val vectorDrawableCompat = VectorDrawableCompat.create(resources, resourceId, null)
            return vectorDrawableCompat!!.mutate() as VectorDrawable
        }


        fun showToast(activity: Activity,textToast:String){

            val inflater: LayoutInflater = activity.layoutInflater
            val layout: View = inflater.inflate(R.layout.custom_toast_layout, activity.findViewById(R.id.custom_toast_layout))

            val text = layout.findViewById<AppCompatTextView>(R.id.toast_text)
            text.text = textToast

            val toast = Toast(activity)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.setDuration(Toast.LENGTH_LONG)
            toast.view = layout
            toast.show()
        }

        // Method to convert URI to File
        fun uriToFile(context: Context, uri: Uri): File? {
            var filePath: String? = null
            var file: File? = null
            // Check if the URI scheme is "file"
            if ("file" == uri.scheme) {
                filePath = uri.path
            } else if ("content" == uri.scheme) { // Check if the URI scheme is "content"
                // Resolve the content URI to get the actual file path
                val projection = arrayOf(MediaStore.Images.Media.DATA)
                val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    filePath = cursor.getString(columnIndex)
                    cursor.close()
                }
            }
            // If the file path is not null, create a File object
            if (filePath != null) {
                file = File(filePath)
            }
            return file
        }

        //for serialization of the drawings
        fun serializeDrawing(paths: ArrayList<DrawingView.CustomPath>): String {
            val serializablePaths = paths.map { it.serialize() }
            return Gson().toJson(serializablePaths)
        }

        fun deserializeDrawing(json: String): ArrayList<DrawingView.CustomPath> {
            val type = object : TypeToken<List<SerializableDrawingPath>>() {}.type
            val serializedPaths: List<SerializableDrawingPath> = Gson().fromJson(json, type)
            return ArrayList(serializedPaths.map { DrawingView.deserialize(it) })
        }

        fun convertPathsToBitmap(paths: ArrayList<DrawingView.CustomPath>,resources:Resources): Bitmap {

            //creating new bitmap
            val bitmap = Bitmap.createBitmap(resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            //drawing paths on the bitmap
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            for (path in paths){

                paint.color = path.color
                paint.strokeWidth = path.brushThickness.toFloat()
                paint.alpha = path.alpha
                //this will draw the stroke and not fill the color inside
                paint.style = Paint.Style.STROKE
                canvas.drawPath(path, paint)
            }

            return bitmap
        }
    }

}