package com.example.imagegallerysaver

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ImageGallerySaverPlugin: MethodCallHandler,FlutterPlugin {
    private var context:Context? = null;
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val plugin = ImageGallerySaverPlugin()
      plugin.context = registrar.context()
      register(plugin,registrar.messenger())
    }

    private fun register(plugin: ImageGallerySaverPlugin, messenger: BinaryMessenger) {
      val channel = MethodChannel(messenger, "image_gallery_saver")
      channel.setMethodCallHandler(plugin)
    }
  }



  override fun onMethodCall(call: MethodCall, result: Result): Unit {
    when {
        call.method == "saveImageToGallery" -> {
          val image = call.argument<ByteArray>("imageBytes") ?: return
          val quality = call.argument<Int>("quality") ?: return
          val name = call.argument<String>("name")

          result.success(saveImageToGallery(BitmapFactory.decodeByteArray(image, 0, image.size), quality, name))
        }
        call.method == "saveFileToGallery" -> {
          val path = call.arguments as String
          result.success(saveFileToGallery(path))
        }
        else -> result.notImplemented()
    }

  }

  private fun generateFile(extension: String = "", name: String? = null): File {
    var storePath =  Environment.getExternalStorageDirectory().absolutePath + File.separator + getApplicationName()
    
       context?.externalMediaDirs?.let {
            if (it.isNotEmpty()) {

                it[0].let { mediaDir ->
                       storePath = "${mediaDir?.absolutePath}/documents/images";
                }
            }
        }
      
      
    val appDir = File(storePath)
    if (!appDir.exists()) {
      appDir.mkdir()
    }
    var fileName = name?:System.currentTimeMillis().toString()
    if (extension.isNotEmpty()) {
      fileName += (".$extension")
    }
    return File(appDir, fileName)
  }

  private fun saveImageToGallery(bmp: Bitmap, quality: Int, name: String?): String {

    val file = generateFile("jpg", name = name)
    try {
      val fos = FileOutputStream(file)
      println("ImageGallerySaverPlugin $quality")
      bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos)
      fos.flush()
      fos.close()
      val uri = Uri.fromFile(file)
      context?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
      bmp.recycle()
      return uri.toString()
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return ""
  }

  private fun saveFileToGallery(filePath: String): String {

    return try {
      val originalFile = File(filePath)
      val file = generateFile(originalFile.extension)
      originalFile.copyTo(file)

      val uri = Uri.fromFile(file)
      context?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
      return uri.toString()
    } catch (e: IOException) {
      e.printStackTrace()
      ""
    }
  }

  private fun getApplicationName(): String {

    var ai: ApplicationInfo? = null
    context?.let {
      try {
        ai = it.packageManager.getApplicationInfo(it.packageName, 0)
      } catch (e: PackageManager.NameNotFoundException) {
      }
    }

    var appName: String = ""

    context?.let {
      appName = if (ai != null) {
        val charSequence = it.packageManager.getApplicationLabel(ai!!)
        StringBuilder(charSequence.length).append(charSequence).toString()
      } else {
        "image_gallery_saver"
      }
    }
    return  appName
  }

  override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    context = binding.applicationContext
    register(this, binding.getBinaryMessenger());
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {

  }


}
