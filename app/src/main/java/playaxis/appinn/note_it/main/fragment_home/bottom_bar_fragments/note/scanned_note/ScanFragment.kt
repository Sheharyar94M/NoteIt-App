package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.note.scanned_note

import android.app.ProgressDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.AnnotateImageRequest
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest
import com.google.api.services.vision.v1.model.Feature
import com.google.api.services.vision.v1.model.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.databinding.FragmentScanBinding
import playaxis.appinn.note_it.main.fragment_home.HomeViewModel
import playaxis.appinn.note_it.main.utils.send
import playaxis.appinn.note_it.main.viewModels.MainViewModel
import java.io.ByteArrayOutputStream
import java.io.IOException


class ScanFragment : Fragment() {

    private lateinit var binding: FragmentScanBinding
    private val viewModelHome: HomeViewModel by activityViewModels()
    private val viewModelMain: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentScanBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create and show the progress dialog
        val progressDialog = ProgressDialog(context, R.style.AppCompatAlertDialogStyle)
        progressDialog.setMessage("Please wait...")
        progressDialog.setCancelable(false)

        //setting the image to the cropper
        binding.cropImageView.setImageUriAsync(Uri.parse(viewModelHome.scanImage))

        binding.startScan.setOnClickListener {

            //progress dialog
            progressDialog.show()

            val cropperImage = binding.cropImageView.croppedImage
            //send the cropped image to the Google vision
            lifecycleScope.launch(Dispatchers.IO) {
                val recognizedBuilder = scanImage(cropperImage)
                //recognized text is sent to the viewModel Home (Create liveData for the text recognized)
                withContext(Dispatchers.Main){

                    // Convert the StringBuilder to a String
                    if (recognizedBuilder != null){
                        viewModelMain.recognizedTextLiveData.send(recognizedBuilder.toString())
                        Log.i("recognizedText: ",recognizedBuilder.toString())
                    }
                    else{
                        Toast.makeText(requireActivity(),"Unable to detect text for a moment!",Toast.LENGTH_SHORT).show()
                    }

                    findNavController().popBackStack()
                    findNavController().popBackStack()
                    progressDialog.dismiss()
                }
            }
        }
    }

    private fun getBase64EncodedJpeg(bitmap: Bitmap): Image {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val jpegByteArray = outputStream.toByteArray()
        val base64EncodedImage = Base64.encodeToString(jpegByteArray, Base64.DEFAULT)

        // Create the Image object with the appropriate constructor
        val image = Image().apply {
            content = base64EncodedImage
        }
        return image
    }
    private fun scanImage(bitmap: Bitmap): StringBuilder? {

        try {

            val httpTransport = AndroidHttp.newCompatibleTransport()
            val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

            // Use the API key in the Vision.Builder
            val builder = Vision.Builder(httpTransport, jsonFactory, null)
            builder.applicationName = "QuickNotepad"
            builder.setVisionRequestInitializer(VisionRequestInitializer(getString(R.string.image_to_text_api_key)))
            val vision = builder.build()

            //Vision work
            val featureList: MutableList<Feature> = ArrayList()

            val textDetection = Feature()
            textDetection.setType("TEXT_DETECTION")
            textDetection.setMaxResults(1)
            featureList.add(textDetection)

            val imageList: MutableList<AnnotateImageRequest> = ArrayList()
            val annotateImageRequest = AnnotateImageRequest()
            val base64EncodedImage: Image = getBase64EncodedJpeg(bitmap)
            annotateImageRequest.setImage(base64EncodedImage)
            annotateImageRequest.setFeatures(featureList)
            imageList.add(annotateImageRequest)

            val batchAnnotateImagesRequest = BatchAnnotateImagesRequest()
            batchAnnotateImagesRequest.setRequests(imageList)
            val annotateRequest = vision.images().annotate(batchAnnotateImagesRequest)
            annotateRequest.setDisableGZipContent(true)

            Log.d("TAG", "Sending request to Google Cloud")

            val responses = annotateRequest.execute()

            // Process the response and extract text annotations
            val textAnnotations = responses.responses?.get(0)?.textAnnotations
            val stringBuilder = StringBuilder()

            if (textAnnotations != null){
                if(textAnnotations.isNotEmpty()){
                    val recognizedText = textAnnotations[0].description
                    stringBuilder.append("$recognizedText\n")
                }
            }

            Log.i("recognizedTextStringBuilder: ", stringBuilder.toString())

            return stringBuilder

        } catch (e: GoogleJsonResponseException) {
            Log.e("Request error: ", e.content)
        } catch (e: IOException) {
            Log.d("Request error: ", e.message!!)
        }
        return null
    }
//
//    override fun onDetach() {
//        super.onDetach()
//
//        viewModelMain.bottomFragmentItemDetachEventEvent()
//    }
}